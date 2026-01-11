package tech.kayys.wayang.mcp.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "mcp.generator")
public interface McpServerConfig {

    @WithDefault("tech.kayys.wayang.mcp.mcp")
    String defaultPackage();

    @WithDefault("GeneratedMcpServer")
    String defaultServerName();

    @WithDefault("http://localhost:8080")
    String defaultBaseUrl();

    @WithDefault("10485760") // 10MB
    long maxFileSize();

    @WithDefault("2.0,3.0,3.1")
    String[] supportedVersions();

    @WithDefault("openapi,postman")
    String[] supportedSpecTypes();
}
