package tech.kayys.wayang.workflow.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request DTO for completing a workflow run
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteRunRequest {
    
    @NotNull
    @JsonProperty("outputs")
    private Map<String, Object> outputs;
}