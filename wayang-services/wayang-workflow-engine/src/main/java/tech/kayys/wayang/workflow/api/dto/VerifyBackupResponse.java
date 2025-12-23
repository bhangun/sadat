package tech.kayys.wayang.workflow.api.dto;

import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class VerifyBackupResponse {
    private String backupId;
    private boolean valid;
    private List<String> issues;
    private Instant verifiedAt;
}
