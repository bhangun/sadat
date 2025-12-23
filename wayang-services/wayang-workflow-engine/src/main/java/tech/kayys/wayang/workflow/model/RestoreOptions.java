package tech.kayys.wayang.workflow.model;

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
public class RestoreOptions {
    private boolean selective;
    private List<String> selectedItems;
    private boolean skipVerification;
    private Map<String, Object> parameters;

    public static RestoreOptions fullRestore() {
        return RestoreOptions.builder()
                .selective(false)
                .selectedItems(List.of())
                .skipVerification(false)
                .parameters(Map.of())
                .build();
    }
}
