


import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class AuditEntry {
    private String id;
    private String event;
    private Instant timestamp;
    private Actor actor;
    private Target target;
    private Map<String, Object> metadata;
    private String hash;
    private String prevHash;
}