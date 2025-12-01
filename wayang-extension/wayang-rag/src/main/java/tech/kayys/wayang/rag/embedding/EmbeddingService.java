package io.wayang.rag.vector.embedding;

import io.quarkus.cache.CacheResult;
import io.quarkus.runtime.Startup;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced Embedding Service with caching, batching, and fallback
 */
@ApplicationScoped
public class EmbeddingService {
    
    private static final Logger LOG = Logger.getLogger(EmbeddingService.class);
    
    @Inject
    EmbeddingProvider embeddingProvider;
    
    @ConfigProperty(name = "wayang.rag.embedding.batchSize", defaultValue = "32")
    int batchSize;
    
    @ConfigProperty(name = "wayang.rag.embedding.enableCaching", defaultValue = "true")
    boolean enableCaching;
    
    @ConfigProperty(name = "wayang.rag.embedding.dimension", defaultValue = "1536")
    int dimension;
    
    /**
     * Embed single text with caching
     */
    @CacheResult(cacheName = "embedding-cache")
    public float[] embed(String text) {
        String normalizedText = normalizeText(text);
        
        try {
            return embeddingProvider.embed(normalizedText);
        } catch (Exception e) {
            LOG.error("Failed to generate embedding", e);
            throw new EmbeddingException("Embedding generation failed", e);
        }
    }
    
    /**
     * Embed multiple texts in batch for efficiency
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (texts.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Normalize all texts
        List<String> normalizedTexts = texts.stream()
                .map(this::normalizeText)
                .toList();
        
        // Check cache first if enabled
        if (enableCaching) {
            return embedBatchWithCache(normalizedTexts);
        }
        
        // Process in batches
        List<float[]> allEmbeddings = new ArrayList<>();
        for (int i = 0; i < normalizedTexts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, normalizedTexts.size());
            List<String> batch = normalizedTexts.subList(i, end);
            
            try {
                List<float[]> batchEmbeddings = embeddingProvider.embedBatch(batch);
                allEmbeddings.addAll(batchEmbeddings);
            } catch (Exception e) {
                LOG.error("Failed to generate batch embeddings", e);
                // Fallback to individual embedding
                for (String text : batch) {
                    allEmbeddings.add(embed(text));
                }
            }
        }
        
        return allEmbeddings;
    }
    
    /**
     * Async embedding for non-blocking operations
     */
    public CompletableFuture<float[]> embedAsync(String text) {
        return CompletableFuture.supplyAsync(() -> embed(text));
    }
    
    /**
     * Embed with metadata for better caching
     */
    @CacheResult(cacheName = "embedding-cache")
    public EmbeddingResult embedWithMetadata(String text) {
        float[] embedding = embed(text);
        
        return EmbeddingResult.builder()
                .embedding(embedding)
                .text(text)
                .dimension(embedding.length)
                .model(embeddingProvider.getModelName())
                .checksum(calculateChecksum(text))
                .build();
    }
    
    /**
     * Get embedding dimension
     */
    public int getDimension() {
        return dimension;
    }
    
    /**
     * Normalize text for consistent embeddings
     */
    private String normalizeText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }
        
        // Normalize whitespace
        text = text.replaceAll("\\s+", " ").trim();
        
        // Limit length (most models have token limits)
        if (text.length() > 8000) {
            text = text.substring(0, 8000);
        }
        
        return text;
    }
    
    /**
     * Batch embedding with cache awareness
     */
    private List<float[]> embedBatchWithCache(List<String> texts) {
        List<float[]> embeddings = new ArrayList<>();
        List<String> uncachedTexts = new ArrayList<>();
        List<Integer> uncachedIndices = new ArrayList<>();
        
        // Check which texts are already cached
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            try {
                float[] cached = embed(text); // Will use cache if available
                embeddings.add(cached);
            } catch (Exception e) {
                uncachedTexts.add(text);
                uncachedIndices.add(i);
                embeddings.add(null); // Placeholder
            }
        }
        
        // Embed uncached texts in batch
        if (!uncachedTexts.isEmpty()) {
            List<float[]> newEmbeddings = embeddingProvider.embedBatch(uncachedTexts);
            
            for (int i = 0; i < uncachedIndices.size(); i++) {
                int index = uncachedIndices.get(i);
                embeddings.set(index, newEmbeddings.get(i));
            }
        }
        
        return embeddings;
    }
    
    /**
     * Calculate checksum for cache key
     */
    private String calculateChecksum(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(text.hashCode());
        }
    }
}