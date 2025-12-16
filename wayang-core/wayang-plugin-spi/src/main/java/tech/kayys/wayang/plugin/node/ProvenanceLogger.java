package tech.kayys.wayang.plugin.node;

import java.nio.channels.Channel;
import java.time.Instant;
import java.util.Map;


/* import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject; */
import tech.kayys.wayang.plugin.ExecutionResult;

/**
 * Provenance logger
 */
// No CDI annotations!
public class ProvenanceLogger {
    private final String nodeId;
    private final Emitter<String> emitter; // injected via constructor

    ProvenanceLogger(String nodeId, Emitter<String> emitter) {
        this.nodeId = nodeId;
        this.emitter = emitter;
    }

    public void log(NodeContext context, ExecutionResult result) {
        var entry = Map.of(
            "nodeId", nodeId,
            "runId", context.getRunId(),
            "tenantId", context.getTenantId(),
            "status", result.getStatus().name(),
            "timestamp", Instant.now().toString(),
            "outputs", result.getOutputs()
        );
        emitter.send(JsonUtils.toJson(entry));
    }
}