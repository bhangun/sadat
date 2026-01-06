
@Path("/api/v1/model")
@RegisterRestClient(configKey = "model-router")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ModelRouterService {
    
    @POST
    @Path("/complete")
    Uni<ModelApiResponse> complete(ModelApiRequest request);
    
    @POST
    @Path("/embed")
    Uni<EmbedApiResponse> embed(EmbedApiRequest request);
    
    @GET
    @Path("/models/{modelId}")
    Uni<ModelInfo> getModel(@PathParam("modelId") String modelId);
}
