package tech.kayys.wayang.workflow.kernel;

import java.time.Instant;
import java.util.Map;

/**
 * 🔒 External signal from outside services
 */
public interface ExternalSignal {

    String getSignalType(); // "callback", "webhook", "timer", "human_approval"

    String getSource(); // External service identifier

    Map<String, Object> getPayload();

    Instant getTimestamp();

    String getSignature(); // For verification
}
