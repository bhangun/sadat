package tech.kayys.wayang.rag.service;

import tech.kayys.wayang.common.domain.*;
import tech.kayys.wayang.rag.repository.VectorRepository;
import tech.kayys.wayang.rag.embeddings.EmbeddingService;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class RAGService {
    
    @Inject
    EmbeddingService embeddingService;
    
    @Inject
    VectorRepository vectorRepository;
    
    @Inject
    ChunkingService chunkingService;
    
    @Inject
    ReRanker reRanker;
    
    /**
     * Index documents for retrieval
     */
    public Uni<IndexResult> indexDocuments(
        String tenantId, 
        List<Document> documents
    ) {
        return Multi.createFrom().iterable(documents)
            .onItem().transformToUniAndMerge(doc -> 
                // Chunk document
                chunkingService.chunk(doc)
                    .onItem().transformToMulti(Multi.createFrom()::iterable)
                    .onItem().transformToUniAndMerge(chunk -> 
                        // Embed chunk
                        embeddingService.embed(chunk.text())
                            .flatMap(embedding -> 
                                // Store in vector DB
                                vectorRepository.store(VectorRecord.builder()
                                    .tenantId(tenantId)
                                    .documentId(doc.id())
                                    .chunkId(chunk.id())
                                    .text(chunk.text())
                                    .embedding(embedding)
                                    .metadata(chunk.metadata())
                                    .build()
                                )
                            )
                    )
            )
            .collect().asList()
            .map(results -> new IndexResult(documents.size(), results.size()));
    }
    
    /**
     * Retrieve relevant chunks for query
     */
    public Uni<RetrievalResult> retrieve(
        String tenantId,
        String query,
        RetrievalOptions options
    ) {
        return embeddingService.embed(query)
            .flatMap(queryEmbedding -> 
                vectorRepository.search(VectorSearchRequest.builder()
                    .tenantId(tenantId)
                    .queryEmbedding(queryEmbedding)
                    .topK(options.topK() * 2) // Fetch more for reranking
                    .filters(options.filters())
                    .build()
                )
            )
            .flatMap(searchResults -> 
                // Rerank results
                reRanker.rerank(query, searchResults, options.topK())
            )
            .map(rankedResults -> new RetrievalResult(
                query,
                rankedResults.size(),
                rankedResults
            ));
    }
    
    /**
     * Hybrid search (vector + keyword)
     */
    public Uni<RetrievalResult> hybridSearch(
        String tenantId,
        String query,
        RetrievalOptions options
    ) {
        return Uni.combine().all()
            .unis(
                // Vector search
                retrieve(tenantId, query, options),
                // Keyword search (BM25)
                vectorRepository.keywordSearch(tenantId, query, options.topK())
            )
            .combinedWith((vectorResults, keywordResults) -> 
                // Fuse results with reciprocal rank fusion
                fuseResults(vectorResults, keywordResults)
            );
    }
    
    private RetrievalResult fuseResults(
        RetrievalResult vector, 
        List<VectorRecord> keyword
    ) {
        // Reciprocal Rank Fusion algorithm
        Map<String, Double> scores =new HashMap<>();
        
        for (int i = 0; i < vector.results().size(); i++) {
            String id = vector.results().get(i).chunkId();
            scores.merge(id, 1.0 / (i + 60), Double::sum);
        }
        
        for (int i = 0; i < keyword.size(); i++) {
            String id = keyword.get(i).chunkId();
            scores.merge(id, 1.0 / (i + 60), Double::sum);
        }
        
        List<VectorRecord> fused = scores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .map(id -> findRecordById(id, vector.results(), keyword))
            .filter(Objects::nonNull)
            .toList();
        
        return new RetrievalResult(vector.query(), fused.size(), fused);
    }
}