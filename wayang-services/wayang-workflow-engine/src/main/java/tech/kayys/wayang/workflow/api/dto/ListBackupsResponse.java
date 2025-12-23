package tech.kayys.wayang.workflow.api.dto;

import java.util.List;
import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class ListBackupsResponse {
    private List<BackupResponse> backups;
    private int totalCount;
}
