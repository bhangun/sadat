package tech.kayys.wayang.workflow.api.dto;

import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeRequest {
    private String humanTaskId;
    private Map<String, Object> resumeData;
}
