package tech.kayys.wayang.workflow.kernel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Information about an available executor.
 * Used for service discovery and load balancing.
 */
@Data
@Builder(toBuilder = true)
public class ExecutorInfo {

    private final String executorId;
    private final String name;
    private final String version;
    private final String endpoint;
    private final Set<String> supportedNodeTypes;
    private final Set<String> capabilities;
    private final Map<String, Object> metadata;
    private final Instant registeredAt;
    private final Instant lastHeartbeat;
    private final ExecutorHealth health;
    private final LoadInfo load;
    private final List<CommunicationProtocol> supportedProtocols;

    @JsonCreator
    public ExecutorInfo(
            @JsonProperty("executorId") String executorId,
            @JsonProperty("name") String name,
            @JsonProperty("version") String version,
            @JsonProperty("endpoint") String endpoint,
            @JsonProperty("supportedNodeTypes") Set<String> supportedNodeTypes,
            @JsonProperty("capabilities") Set<String> capabilities,
            @JsonProperty("metadata") Map<String, Object> metadata,
            @JsonProperty("registeredAt") Instant registeredAt,
            @JsonProperty("lastHeartbeat") Instant lastHeartbeat,
            @JsonProperty("health") ExecutorHealth health,
            @JsonProperty("load") LoadInfo load,
            @JsonProperty("supportedProtocols") List<CommunicationProtocol> supportedProtocols) {

        this.executorId = executorId;
        this.name = name;
        this.version = version;
        this.endpoint = endpoint;
        this.supportedNodeTypes = supportedNodeTypes != null ? Set.copyOf(supportedNodeTypes) : Set.of();
        this.capabilities = capabilities != null ? Set.copyOf(capabilities) : Set.of();
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        this.registeredAt = registeredAt != null ? registeredAt : Instant.now();
        this.lastHeartbeat = lastHeartbeat;
        this.health = health;
        this.load = load;
        this.supportedProtocols = supportedProtocols != null ? List.copyOf(supportedProtocols) : List.of();
    }

    // Factory methods
    public static ExecutorInfo create(
            String executorId,
            String endpoint,
            Set<String> supportedNodeTypes,
            Set<String> capabilities) {

        return ExecutorInfo.builder()
                .executorId(executorId)
                .name(executorId)
                .version("1.0.0")
                .endpoint(endpoint)
                .supportedNodeTypes(supportedNodeTypes)
                .capabilities(capabilities)
                .registeredAt(Instant.now())
                .health(ExecutorHealth.healthy(executorId))
                .load(LoadInfo.idle())
                .supportedProtocols(List.of(CommunicationProtocol.HTTP))
                .build();
    }

    public static ExecutorInfo forAiAgent(
            String agentId,
            String endpoint,
            AiAgentType agentType,
            Set<String> additionalCapabilities) {

        Set<String> capabilities = new java.util.HashSet<>(agentType.getCapabilities());
        if (additionalCapabilities != null) {
            capabilities.addAll(additionalCapabilities);
        }

        return ExecutorInfo.builder()
                .executorId("ai-" + agentId)
                .name(agentType.getType() + "-agent")
                .version("1.0.0")
                .endpoint(endpoint)
                .supportedNodeTypes(Set.of("ai." + agentType.getType()))
                .capabilities(capabilities)
                .metadata(Map.of(
                        "agentType", agentType.getType(),
                        "category", "ai-agent",
                        "aiProvider", "multiple"))
                .registeredAt(Instant.now())
                .health(ExecutorHealth.healthy("ai-" + agentId))
                .load(LoadInfo.idle())
                .supportedProtocols(List.of(
                        CommunicationProtocol.HTTP,
                        CommunicationProtocol.GRPC,
                        CommunicationProtocol.KAFKA))
                .build();
    }

    public boolean supportsNodeType(String nodeType) {
        return supportedNodeTypes.contains(nodeType);
    }

    public boolean hasCapability(String capability) {
        return capabilities.contains(capability);
    }

    public boolean isAvailable() {
        if (health == null) {
            return false;
        }

        // Check if executor is operational
        if (!health.isOperational()) {
            return false;
        }

        // Check if executor is overloaded
        if (load != null && load.isOverloaded()) {
            return false;
        }

        // Check if heartbeat is recent (within 5 minutes)
        if (lastHeartbeat != null) {
            Duration sinceLastHeartbeat = Duration.between(lastHeartbeat, Instant.now());
            if (sinceLastHeartbeat.toMinutes() > 5) {
                return false;
            }
        }

        return true;
    }

    public boolean supportsProtocol(CommunicationProtocol protocol) {
        return supportedProtocols.contains(protocol);
    }

    @Data
    @Builder
    public static class LoadInfo {
        private final int currentConnections;
        private final int maxConnections;
        private final double cpuUsage;
        private final double memoryUsage;
        private final int queueSize;
        private final Instant measuredAt;

        public static LoadInfo idle() {
            return LoadInfo.builder()
                    .currentConnections(0)
                    .maxConnections(100)
                    .cpuUsage(0.1)
                    .memoryUsage(0.2)
                    .queueSize(0)
                    .measuredAt(Instant.now())
                    .build();
        }

        public boolean isOverloaded() {
            // Consider overloaded if connections > 80% of max or CPU > 80%
            return (double) currentConnections / maxConnections > 0.8 ||
                    cpuUsage > 0.8;
        }

        public double getLoadPercentage() {
            double connectionLoad = (double) currentConnections / maxConnections;
            return Math.max(connectionLoad, cpuUsage);
        }
    }

    public enum CommunicationProtocol {
        HTTP, GRPC, KAFKA, WEBSOCKET, AMQP
    }
}
