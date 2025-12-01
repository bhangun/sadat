package tech.kayys.wayang.resource;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import tech.kayys.wayang.model.ModelInfo;
import tech.kayys.wayang.model.ModelListResponse;
import tech.kayys.wayang.model.ModelResponse;
import tech.kayys.wayang.plugin.ModelManager;

public class ModelsResource {
    
    @Inject
    ModelManager modelManager;
    
    @GET
    public ModelListResponse listModels() {
        List<ModelResponse> models = modelManager.listModels().entrySet().stream()
            .map(entry -> modelInfoToResponse(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
        
        return new ModelListResponse("list", models);
    }
    
    @GET
    @Path("/{model}")
    public ModelResponse getModel(@PathParam("model") String modelId) {
        ModelInfo info = modelManager.listModels().get(modelId);
        if (info == null) {
            throw new NotFoundException("Model not found: " + modelId);
        }
        return modelInfoToResponse(modelId, info);
    }
    
    private ModelResponse modelInfoToResponse(String id, ModelInfo info) {
        return new ModelResponse(
            id,
            "model",
            System.currentTimeMillis() / 1000,
            "llamajava",
            info
        );
    }
}
