package tech.kayys.wayang.workflow.sdk;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.util.Map;

@Data
@Builder(toBuilder = true)
public class ClientConfig {
    @Builder.Default
    private String baseUrl = "http://localhost:8080";

    @Builder.Default
    private Duration connectTimeout = Duration.ofSeconds(30);

    @Builder.Default
    private Duration readTimeout = Duration.ofSeconds(60);

    @Builder.Default
    private Duration writeTimeout = Duration.ofSeconds(30);

    @Builder.Default
    private Integer maxRetries = 3;

    @Builder.Default
    private Duration retryDelay = Duration.ofMillis(500);

    @Builder.Default
    private Boolean enableCompression = true;

    @Builder.Default
    private Boolean enableMetrics = true;

    @Builder.Default
    private Boolean enableTracing = true;

    private String apiKey;
    private String apiSecret;
    private String tenantId;

    @Builder.Default
    private Map<String, String> headers = Map.of(
            "User-Agent", "Workflow-Client-SDK/1.0.0",
            "Content-Type", "application/json");

    @Builder.Default
    private EventStreamConfig eventStreamConfig = EventStreamConfig.defaultConfig();

    @Data
    @Builder
    public static class EventStreamConfig {
        @Builder.Default
        private Duration reconnectDelay = Duration.ofSeconds(5);

        @Builder.Default
        private Integer maxReconnectAttempts = 10;

        @Builder.Default
        private Duration pingInterval = Duration.ofSeconds(30);

        @Builder.Default
        private Duration connectionTimeout = Duration.ofSeconds(10);

        public static EventStreamConfig defaultConfig() {
            return EventStreamConfig.builder().build();
        }
    }
}
