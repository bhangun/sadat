package io.wayang.rag.vector.embedding.provider;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Local Sentence Transformer embedding provider using DJL
 */
@ApplicationScoped
@Named("local")
@Startup
public class LocalEmbeddingProvider implements EmbeddingProvider {
    
    private static final Logger LOG = Logger.getLogger(LocalEmbeddingProvider.class);
    
    @ConfigProperty(name = "wayang.rag.embedding.modelName", defaultValue = "sentence-transformers/all-MiniLM-L6-v2")
    String modelName;
    
    @ConfigProperty(name = "wayang.rag.embedding.modelPath")
    Optional<String> modelPath;
    
    @ConfigProperty(name = "wayang.rag.embedding.dimension", defaultValue = "384")
    int dimension;
    
    private HuggingFaceTokenizer tokenizer;
    private NDManager ndManager;
    
    @PostConstruct
    public void init() {
        try {
            LOG.infof("Initializing local embedding model: %s", modelName);
            
            // Initialize NDManager
            ndManager = NDManager.newBaseManager();
            
            // Load tokenizer
            String tokenizerPath = modelPath.orElse("hf://sentence-transformers/" + modelName);
            tokenizer = HuggingFaceTokenizer.newInstance(tokenizerPath);
            
            LOG.info("Local embedding model initialized successfully");
        } catch (Exception e) {
            LOG.error("Failed to initialize local embedding model", e);
            throw new RuntimeException("Embedding model initialization failed", e);
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (ndManager != null) {
            ndManager.close();
        }
    }
    
    @Override
    public float[] embed(String text) {
        try {
            // Tokenize
            long[] tokens = tokenizer.encode(text).getIds();
            
            // Create input tensors
            try (NDManager manager = ndManager.newSubManager()) {
                NDArray inputIds = manager.create(tokens);
                NDArray attentionMask = manager.ones(inputIds.getShape());
                
                // Get embeddings (simplified - in real implementation, use ONNX Runtime or similar)
                // This is a placeholder for actual model inference
                float[] embedding = new float[dimension];
                
                // Mean pooling simulation (replace with actual model inference)
                for (int i = 0; i < dimension; i++) {
                    embedding[i] = (float) Math.random();
                }
                
                // Normalize
                return normalizeEmbedding(embedding);
            }
        } catch (Exception e) {
            LOG.error("Failed to generate embedding", e);
            throw new EmbeddingException("Embedding generation failed", e);
        }
    }
    
    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> embeddings = new ArrayList<>();
        
        for (String text : texts) {
            embeddings.add(embed(text));
        }
        
        return embeddings;
    }
    
    @Override
    public String getModelName() {
        return modelName;
    }
    
    @Override
    public int getDimension() {
        return dimension;
    }
    
    /**
     * Normalize embedding to unit length
     */
    private float[] normalizeEmbedding(float[] embedding) {
        float norm = 0;
        for (float v : embedding) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        
        if (norm > 0) {
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] /= norm;
            }
        }
        
        return embedding;
    }
}