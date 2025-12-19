package tech.kayys.wayang.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for Workflow
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowDTO {

    private UUID id;

    @NotBlank(message = "Workflow name is required")
    @Size(max = 255, message = "Workflow name must not exceed 255 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private UUID workspaceId;
    private UUID tenantId;
    private Workflow.WorkflowStatus status;
    private List<NodeDTO> nodes;
    private List<EdgeDTO> edges;
    private LayoutDTO layout;
    private ValidationStateDTO validationState;
    private ConfigurationDTO configuration;
    private List<String> tags;
    private String publishedVersion;
    private Instant publishedAt;
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
    private Long version;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationStateDTO {
        private Boolean isValid;
        private Instant lastValidatedAt;
        private List<ValidationErrorDTO> errors;
        private List<ValidationWarningDTO> warnings;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationErrorDTO {
        private String code;
        private String message;
        private String nodeId;
        private String field;
        private String severity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationWarningDTO {
        private String code;
        private String message;
        private String nodeId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ConfigurationDTO {
        private Integer maxConcurrency;
        private Integer timeout;
        private String retryPolicy;
        private Boolean enableCheckpoints;
        private Boolean enableStreaming;
    }

    // Conversion methods
    public static WorkflowDTO from(WorkflowDefinition workflow, boolean includeNodes) {
        if (workflow == null)
            return null;

        WorkflowDTOBuilder builder = WorkflowDTO.builder()
                .id(workflow.getId())
                .name(workflow.getName())
                .description(workflow.getDescription())
                .workspaceId(workflow.getWorkspace() != null ? workflow.getWorkspace().getId() : null)
                .tenantId(workflow.getTenantId())
                .status(workflow.getStatus())
                .tags(workflow.getTags())
                .publishedVersion(workflow.getPublishedVersion())
                .publishedAt(workflow.getPublishedAt())
                .createdAt(workflow.getCreatedAt())
                .createdBy(workflow.getCreatedBy())
                .updatedAt(workflow.getUpdatedAt())
                .updatedBy(workflow.getUpdatedBy())
                .version(workflow.getVersion());

        // Include nodes and edges if requested (can be expensive)
        if (includeNodes && workflow.getNodes() != null) {
            builder.nodes(workflow.getNodes().stream()
                    .map(NodeDTO::from)
                    .toList());
        }

        if (includeNodes && workflow.getEdges() != null) {
            builder.edges(workflow.getEdges().stream()
                    .map(EdgeDTO::from)
                    .toList());
        }

        if (workflow.getLayout() != null) {
            builder.layout(LayoutDTO.from(workflow.getLayout()));
        }

        // Validation state
        if (workflow.getValidationState() != null) {
            builder.validationState(convertValidationState(workflow.getValidationState()));
        }

        // Configuration
        if (workflow.getConfiguration() != null) {
            builder.configuration(convertConfiguration(workflow.getConfiguration()));
        }

        return builder.build();
    }

    private static ValidationStateDTO convertValidationState(Workflow.ValidationState state) {
        if (state == null)
            return null;

        List<ValidationErrorDTO> errors = null;
        if (state.getErrors() != null) {
            errors = state.getErrors().stream()
                    .map(e -> ValidationErrorDTO.builder()
                            .code(e.getCode())
                            .message(e.getMessage())
                            .nodeId(e.getNodeId())
                            .field(e.getField())
                            .severity(e.getSeverity())
                            .build())
                    .toList();
        }

        List<ValidationWarningDTO> warnings = null;
        if (state.getWarnings() != null) {
            warnings = state.getWarnings().stream()
                    .map(w -> ValidationWarningDTO.builder()
                            .code(w.getCode())
                            .message(w.getMessage())
                            .nodeId(w.getNodeId())
                            .build())
                    .toList();
        }

        return ValidationStateDTO.builder()
                .isValid(state.getIsValid())
                .lastValidatedAt(state.getLastValidatedAt())
                .errors(errors)
                .warnings(warnings)
                .build();
    }

    private static ConfigurationDTO convertConfiguration(Workflow.WorkflowConfiguration config) {
        if (config == null)
            return null;

        return ConfigurationDTO.builder()
                .maxConcurrency(config.getMaxConcurrency())
                .timeout(config.getTimeout())
                .retryPolicy(config.getRetryPolicy())
                .enableCheckpoints(config.getEnableCheckpoints())
                .enableStreaming(config.getEnableStreaming())
                .build();
    }
}