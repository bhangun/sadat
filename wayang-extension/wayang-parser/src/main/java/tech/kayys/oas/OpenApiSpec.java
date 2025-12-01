

// tech.kayys.platform.tool.OpenApiSpec.java
package tech.kayys.platform.tool;

import io.swagger.v3.oas.models.OpenAPI;
import java.net.URI;
import java.util.List;

public record OpenApiSpec(
    OpenAPI openApi,
    List<URI> servers,
    URI sourceUrl
) {}