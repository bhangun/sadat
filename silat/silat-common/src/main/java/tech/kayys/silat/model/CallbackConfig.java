package tech.kayys.silat.model;

import java.time.Duration;
import java.util.Map;

/**
 * Callback Configuration
 */

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Configuration for external callbacks.
 * Used when workflow needs to wait for external signals.
 */
@Data
@Builder(toBuilder = true)
public class CallbackConfig {

        @Builder.Default
        private final Duration timeout = Duration.ofHours(24);

        @Builder.Default
        private final int maxRetries = 3;

        @Builder.Default
        private final Duration retryDelay = Duration.ofMinutes(5);

        private final String callbackUrl;
        private final Map<String, String> headers;
        private final Map<String, Object> expectedPayload;
        private final String validationSchema; // JSON Schema for payload validation

        @Builder.Default
        private final CallbackMethod method = CallbackMethod.POST;

        @Builder.Default
        private final String contentType = "application/json";

        @Builder.Default
        private final boolean awaitResponse = true;

        @Builder.Default
        private final Map<String, Object> metadata = Map.of();

        @JsonCreator
        public CallbackConfig(
                        @JsonProperty("timeout") Duration timeout,
                        @JsonProperty("maxRetries") int maxRetries,
                        @JsonProperty("retryDelay") Duration retryDelay,
                        @JsonProperty("callbackUrl") String callbackUrl,
                        @JsonProperty("headers") Map<String, String> headers,
                        @JsonProperty("expectedPayload") Map<String, Object> expectedPayload,
                        @JsonProperty("validationSchema") String validationSchema,
                        @JsonProperty("method") CallbackMethod method,
                        @JsonProperty("contentType") String contentType,
                        @JsonProperty("awaitResponse") boolean awaitResponse,
                        @JsonProperty("metadata") Map<String, Object> metadata) {

                this.timeout = timeout != null ? timeout : Duration.ofHours(24);
                this.maxRetries = maxRetries;
                this.retryDelay = retryDelay != null ? retryDelay : Duration.ofMinutes(5);
                this.callbackUrl = callbackUrl;
                this.headers = headers != null ? Map.copyOf(headers) : Map.of();
                this.expectedPayload = expectedPayload != null ? Map.copyOf(expectedPayload) : Map.of();
                this.validationSchema = validationSchema;
                this.method = method != null ? method : CallbackMethod.POST;
                this.contentType = contentType != null ? contentType : "application/json";
                this.awaitResponse = awaitResponse;
                this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        }

        public enum CallbackMethod {
                GET, POST, PUT, PATCH
        }

        // Factory methods
        public static CallbackConfig webhook(String url) {
                return CallbackConfig.builder()
                                .callbackUrl(url)
                                .timeout(Duration.ofMinutes(30))
                                .headers(Map.of(
                                                "Content-Type", "application/json",
                                                "User-Agent", "WorkflowEngine/1.0"))
                                .build();
        }

        public static CallbackConfig humanApproval(String approvalUrl) {
                return CallbackConfig.builder()
                                .callbackUrl(approvalUrl)
                                .timeout(Duration.ofDays(7)) // Give users a week to approve
                                .expectedPayload(Map.of(
                                                "action", "approve|reject",
                                                "approver", String.class,
                                                "comment", String.class))
                                .headers(Map.of(
                                                "Content-Type", "application/json",
                                                "X-Approval-Required", "true"))
                                .metadata(Map.of("type", "human_approval"))
                                .build();
        }

        public static CallbackConfig externalService(String serviceUrl, Map<String, String> authHeaders) {
                Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Content-Type", "application/json");
                if (authHeaders != null) {
                        headers.putAll(authHeaders);
                }

                return CallbackConfig.builder()
                                .callbackUrl(serviceUrl)
                                .timeout(Duration.ofMinutes(10))
                                .headers(headers)
                                .maxRetries(5)
                                .retryDelay(Duration.ofSeconds(30))
                                .metadata(Map.of("type", "external_service"))
                                .build();
        }

        public boolean requiresAuthentication() {
                return headers.containsKey("Authorization") ||
                                headers.containsKey("X-API-Key");
        }

        public boolean hasExpectedPayload() {
                return expectedPayload != null && !expectedPayload.isEmpty();
        }
}
