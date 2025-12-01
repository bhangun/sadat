package tech.kayys.wayang.resource;

import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.WebApplicationException;
import tech.kayys.wayang.model.EmbeddingRequest;
import tech.kayys.wayang.model.EmbeddingResponse;
import tech.kayys.wayang.model.EmbeddingResult;
import tech.kayys.wayang.plugin.ModelManager;

public class EmbeddingsResource {
    private static final Logger log = Logger.getLogger(EmbeddingsResource.class);
    
    @Inject
    ModelManager modelManager;
    
    @POST
    public EmbeddingResponse createEmbedding(EmbeddingRequest request) {
        log.infof("Embedding request: input size=%d", 
            request.input() instanceof List ? ((List<?>) request.input()).size() : 1);
        
        try {
            List<String> texts = new ArrayList<>();
            if (request.input() instanceof String) {
                texts.add((String) request.input());
            } else if (request.input() instanceof List) {
                ((List<?>) request.input()).forEach(item -> texts.add(item.toString()));
            }
            
            EmbeddingResult result = modelManager.getActiveModel().embeddings(texts);
            
            List<EmbeddingResponse.Embedding> embeddings = new ArrayList<>();
            for (int i = 0; i < result.embeddings().size(); i++) {
                embeddings.add(new EmbeddingResponse.Embedding(
                    "embedding", i, result.embeddings().get(i)
                ));
            }
            
            int totalTokens = texts.stream().mapToInt(String::length).sum();
            
            return new EmbeddingResponse(
                "list",
                embeddings,
                modelManager.getActiveModel().getModelInfo().name(),
                new EmbeddingResponse.Usage(totalTokens, totalTokens)
            );
            
        } catch (Exception e) {
            log.error("Embedding generation failed", e);
            throw new WebApplicationException("Embedding generation failed: " + e.getMessage(), 500);
        }
    }
}
