package tech.kayys.wayang.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.kayys.wayang.domain.Workspace;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for Workspace
 * 
 * Used for API requests/responses with appropriate validation and
 * serialization rules. Separated from domain model for clean API contracts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkspaceDTO {

    private UUID id;

    @NotBlank(message = "Workspace name is required")
    @Size(max = 255, message = "Workspace name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private UUID tenantId;
    private String ownerId;
    private Workspace.WorkspaceStatus status;
    private List<String> tags;
    private MetadataDTO metadata;
    private Integer workflowCount;
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
    public static class MetadataDTO {
        private String icon;
        private String color;
        private Boolean favorite;
        private Instant lastAccessedAt;
    }

    // Factory methods for conversion
    public static WorkspaceDTO from(Workspace workspace) {
        if (workspace == null)
            return null;

        MetadataDTO metadataDTO = null;
        if (workspace.getMetadata() != null) {
            metadataDTO = MetadataDTO.builder()
                    .icon(workspace.getMetadata().getIcon())
                    .color(workspace.getMetadata().getColor())
                    .favorite(workspace.getMetadata().getFavorite())
                    .lastAccessedAt(workspace.getMetadata().getLastAccessedAt())
                    .build();
        }

        return WorkspaceDTO.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .tenantId(workspace.getTenantId())
                .ownerId(workspace.getOwnerId())
                .status(workspace.getStatus())
                .tags(workspace.getTags())
                .metadata(metadataDTO)
                .workflowCount(workspace.getWorkflows() != null ? workspace.getWorkflows().size() : 0)
                .createdAt(workspace.getCreatedAt())
                .createdBy(workspace.getCreatedBy())
                .updatedAt(workspace.getUpdatedAt())
                .updatedBy(workspace.getUpdatedBy())
                .version(workspace.getVersion())
                .build();
    }

    public Workspace toEntity() {
        Workspace.WorkspaceMetadata entityMetadata = null;
        if (this.metadata != null) {
            entityMetadata = new Workspace.WorkspaceMetadata();
            entityMetadata.setIcon(this.metadata.getIcon());
            entityMetadata.setColor(this.metadata.getColor());
            entityMetadata.setFavorite(this.metadata.getFavorite());
            entityMetadata.setLastAccessedAt(this.metadata.getLastAccessedAt());
        }

        return Workspace.builder()
                .id(this.id)
                .name(this.name)
                .description(this.description)
                .tenantId(this.tenantId)
                .ownerId(this.ownerId)
                .status(this.status != null ? this.status : Workspace.WorkspaceStatus.ACTIVE)
                .tags(this.tags)
                .metadata(entityMetadata)
                .createdBy(this.createdBy)
                .updatedBy(this.updatedBy)
                .build();
    }
}
