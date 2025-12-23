package tech.kayys.wayang.workflow.api.dto;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RestoreRequest {
    private Boolean selective;
    private List<String> selectedItems;
    private Boolean skipVerification;
    private Map<String, Object> parameters;
}
