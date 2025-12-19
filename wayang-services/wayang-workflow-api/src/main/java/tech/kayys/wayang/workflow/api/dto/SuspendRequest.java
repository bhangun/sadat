package tech.kayys.wayang.workflow.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuspendRequest {
    @NotNull
    private String reason;
    private String humanTaskId;
}
