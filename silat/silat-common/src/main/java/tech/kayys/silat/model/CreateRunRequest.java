package tech.kayys.silat.model;

import java.util.Map;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRunRequest {
    @NotNull
    private String workflowId;

    @NotNull
    private String workflowVersion;

    private Map<String, Object> inputs;
    private String correlationId;

    @Builder.Default
    private boolean autoStart = true;
}
