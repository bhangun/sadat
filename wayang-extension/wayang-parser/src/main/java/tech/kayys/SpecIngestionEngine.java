

// tech.kayys.platform.tool.SpecIngestionEngine.java
package tech.kayys.platform.tool;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.servers.Server;

import java.net.URI;
import java.util.*;

public class SpecIngestionEngine {
    private final HttpClient httpClient;

    public SpecIngestionEngine(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public OpenApiSpec ingest(URI specUrl) throws Exception {
        // Fetch spec
        String specContent = httpClient.send(
            HttpRequest.newBuilder(specUrl).build(),
            HttpResponse.BodyHandlers.ofString()
        ).body();

        // Parse OpenAPI
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(specContent);
        if (result.getMessages() != null && !result.getMessages().isEmpty()) {
            throw new IllegalArgumentException("OpenAPI parse errors: " + result.getMessages());
        }

        OpenAPI openApi = result.getOpenAPI();
        if (openApi == null) {
            throw new IllegalArgumentException("Invalid OpenAPI spec");
        }

        // Resolve servers
        List<URI> servers = openApi.getServers() != null
            ? openApi.getServers().stream()
                .map(Server::getUrl)
                .map(URI::create)
                .collect(Collectors.toList())
            : List.of(specUrl.resolve("/")); // fallback

        return new OpenApiSpec(openApi, servers, specUrl);
    }
}