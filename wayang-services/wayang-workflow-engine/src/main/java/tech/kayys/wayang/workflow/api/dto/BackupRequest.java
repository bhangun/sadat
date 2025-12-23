package tech.kayys.wayang.workflow.api.dto;

import java.util.Map;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BackupRequest {
    private Boolean encrypt;
    private Boolean compress;
    private Map<String, String> metadata;
}
