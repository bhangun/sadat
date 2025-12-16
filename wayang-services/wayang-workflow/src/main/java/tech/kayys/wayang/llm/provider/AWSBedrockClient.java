package tech.kayys.wayang.llm.provider;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface AWSBedrockClient {

    @POST
    @Path("/model/{model}/invoke")
    Uni<JsonObject> invokeModel(
            @PathParam("model") String model,
            JsonObject body);
}
