package tech.kayys.wayang.workflow.kernel;

import java.util.Map;
import java.util.Set;

/**
 * 🔒 Node descriptor - semantic carrier.
 * Kernel treats this as opaque JSON.
 */
public interface NodeDescriptor {

    String getNodeId();

    String getType(); // "task", "gateway", "wait", "start", "end"

    String getImplementation(); // Plugin key: "agent.openai", "http.request", "timer"

    // Opaque configuration (interpreted by plugins)
    Map<String, Object> getConfig();

    // Capabilities hint for orchestration
    Set<String> getCapabilities(); // "retryable", "long-running", "side-effect", "compensatable"

    // UI/editor hints (ignored by kernel)
    Map<String, Object> getUiHints();

    // Validation schema (for DSL/UI)
    String getInputSchema();

    String getOutputSchema();
}
