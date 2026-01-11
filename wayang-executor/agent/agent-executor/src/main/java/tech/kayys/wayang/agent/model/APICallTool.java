package tech.kayys.wayang.agent.model;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * API Call Tool
 * Makes HTTP requests to external APIs
 */
@ApplicationScoped
public class APICallTool extends AbstractTool {

        private static final Logger LOG = LoggerFactory.getLogger(APICallTool.class);

        public APICallTool() {
                super("api_call", "Makes HTTP requests to external APIs. " +
                                "Supports GET, POST, PUT, DELETE methods.");
        }

        @Override
        public Map<String, Object> parameterSchema() {
                return Map.of(
                                "type", "object",
                                "properties", Map.of(
                                                "url", Map.of(
                                                                "type", "string",
                                                                "description", "API endpoint URL"),
                                                "method", Map.of(
                                                                "type", "string",
                                                                "description", "HTTP method",
                                                                "enum", List.of("GET", "POST", "PUT", "DELETE"),
                                                                "default", "GET"),
                                                "headers", Map.of(
                                                                "type", "object",
                                                                "description", "HTTP headers"),
                                                "body", Map.of(
                                                                "type", "object",
                                                                "description", "Request body (for POST/PUT)")),
                                "required", List.of("url"));
        }

        @Override
        public Uni<String> execute(Map<String, Object> arguments, AgentContext context) {
                String url = getParam(arguments, "url", String.class);
                String method = getParamOrDefault(arguments, "method", "GET");

                LOG.debug("API call: {} {}", method, url);

                // In production, use proper HTTP client
                // For now, return placeholder
                return Uni.createFrom().item(
                                "API call to " + url + " completed. Response: [placeholder]");
        }

        @Override
        public boolean isAsync() {
                return true; // HTTP calls can be long-running
        }
}