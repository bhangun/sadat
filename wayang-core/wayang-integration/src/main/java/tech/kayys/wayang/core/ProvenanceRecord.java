



import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class ProvenanceRecord {
    private String id;
    private String runId;
    private String nodeId;
    private String eventType;
    private Instant timestamp;
    private String tenantId;
    private Map<String, Object> metadata;
}