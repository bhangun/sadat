package tech.kayys.wayang.agent.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    @GET
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "AI Agent Runtime",
                "version", "1.0.0");
    }
}