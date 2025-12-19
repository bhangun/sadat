package tech.kayys.wayang.workflow.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;

import tech.kayys.wayang.schema.execution.ErrorPayload;

/**
 * Request DTO for failing a workflow run
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FailRunRequest {
    
    @NotNull
    @JsonProperty("error")
    private ErrorPayload error;
}