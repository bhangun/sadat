package tech.kayys.wayang.resource;

import java.util.List;
import jakarta.ws.rs.*;
import java.util.Map;

public class ModelResource {
    
    @GET
    public Map<String, Object> listModels() {
        return Map.of(
            "object", "list",
            "data", List.of(
                Map.of(
                    "id", "llama-2-7b-chat",
                    "object", "model",
                    "created", System.currentTimeMillis() / 1000,
                    "owned_by", "meta"
                )
            )
        );
    }
}
