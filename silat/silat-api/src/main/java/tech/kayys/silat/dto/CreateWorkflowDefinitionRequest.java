package tech.kayys.silat.dto;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Request to create a workflow definition
 */
@Schema(description = "Request to create a workflow definition")
public record CreateWorkflowDefinitionRequest(
        @NotBlank @Schema(description = "Workflow name", required = true) String name,

        @NotBlank @Schema(description = "Version", required = true) String version,

        @Schema(description = "Description") String description,

        @NotEmpty @Schema(description = "Node definitions", required = true) List<NodeDefinitionDto> nodes,

        @Schema(description = "Input definitions") Map<String, InputDefinitionDto> inputs,

        @Schema(description = "Output definitions") Map<String, OutputDefinitionDto> outputs,

        @Schema(description = "Default retry policy") RetryPolicyDto retryPolicy,

        @Schema(description = "Compensation policy") CompensationPolicyDto compensationPolicy,

        @Schema(description = "Metadata") Map<String, String> metadata) {
}
