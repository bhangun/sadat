
@Path("/api/v1/http")
@RegisterRestClient(configKey = "http-client")
public interface HttpClientService {
    
    @POST
    @Path("/execute")
    Uni<HttpResponse> execute(HttpRequest request);
}
