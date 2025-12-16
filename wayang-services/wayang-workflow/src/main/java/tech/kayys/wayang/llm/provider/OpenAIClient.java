package tech.kayys.wayang.llm.provider;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface OpenAIClient {

    @POST
    Uni<JsonObject> chatCompletions(
            @HeaderParam("Authorization") String authorization,
            JsonObject body);
}
