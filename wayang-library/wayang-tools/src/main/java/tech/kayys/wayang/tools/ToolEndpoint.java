@Value
@Builder
public class ToolEndpoint {
    String url;
    HttpMethod method;
    Map<String, String> headers;
    AuthType authType;
}