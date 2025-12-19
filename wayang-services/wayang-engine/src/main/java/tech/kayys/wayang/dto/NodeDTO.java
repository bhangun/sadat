package tech.kayys.wayang.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.kayys.wayang.common.spi.Node;
import tech.kayys.wayang.schema.node.NodeUI;
import tech.kayys.wayang.schema.node.Outputs;
import tech.kayys.wayang.schema.node.PortDescriptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object for Node
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeDTO {

    private UUID id;

    @NotBlank(message = "Node ID is required")
    private String nodeId;

    @NotBlank(message = "Node name is required")
    private String name;

    private String description;

    @NotBlank(message = "Node type is required")
    private String nodeType;

    private String nodeDescriptorVersion;
    private Map<String, Object> config;
    private List<PortDescriptor> inputs;
    private List<Outputs> outputs;
    private Map<String, Object> properties;
    private PositionDTO position;
    private UIMetadataDTO uiMetadata;
    private Node.NodeStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PositionDTO {
        private Double x;
        private Double y;
        private Double width;
        private Double height;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UIMetadataDTO {
        private String icon;
        private String color;
        private Boolean collapsed;
        private String layer;
    }

    public static NodeDTO from(Node node) {
        if (node == null)
            return null;

        PositionDTO positionDTO = null;
        if (node.getPosition() != null) {
            positionDTO = PositionDTO.builder()
                    .x(node.getPosition().getX())
                    .y(node.getPosition().getY())
                    .width(node.getPosition().getWidth())
                    .height(node.getPosition().getHeight())
                    .build();
        }

        UIMetadataDTO uiMetadataDTO = null;
        if (node.getUiMetadata() != null) {
            uiMetadataDTO = UIMetadataDTO.builder()
                    .icon(node.getUiMetadata().getIcon())
                    .color(node.getUiMetadata().getColor())
                    .collapsed(node.getUiMetadata().getCollapsed())
                    .layer(node.getUiMetadata().getLayer())
                    .build();
        }

        return NodeDTO.builder()
                .id(node.getId())
                .nodeId(node.getNodeId())
                .name(node.getName())
                .description(node.getDescription())
                .nodeType(node.getNodeType())
                .nodeDescriptorVersion(node.getNodeDescriptorVersion())
                .config(node.getConfig())
                .inputs(node.getInputs())
                .outputs(node.getOutputs())
                .properties(node.getProperties())
                .position(positionDTO)
                .uiMetadata(uiMetadataDTO)
                .status(node.getStatus())
                .createdAt(node.getCreatedAt())
                .updatedAt(node.getUpdatedAt())
                .version(node.getVersion())
                .build();
    }

    public Node toEntity() {
        Node.NodePosition entityPosition = null;
        if (this.position != null) {
            entityPosition = new Node.NodePosition();
            entityPosition.setX(this.position.getX());
            entityPosition.setY(this.position.getY());
            entityPosition.setWidth(this.position.getWidth());
            entityPosition.setHeight(this.position.getHeight());
        }

        Node.NodeUIMetadata entityUIMetadata = null;
        if (this.uiMetadata != null) {
            entityUIMetadata = new Node.NodeUIMetadata();
            entityUIMetadata.setIcon(this.uiMetadata.getIcon());
            entityUIMetadata.setColor(this.uiMetadata.getColor());
            entityUIMetadata.setCollapsed(this.uiMetadata.getCollapsed());
            entityUIMetadata.setLayer(this.uiMetadata.getLayer());
        }

        return Node.builder()
                .id(this.id)
                .nodeId(this.nodeId)
                .name(this.name)
                .description(this.description)
                .nodeType(this.nodeType)
                .nodeDescriptorVersion(this.nodeDescriptorVersion)
                .config(this.config)
                .inputs(this.inputs)
                .outputs(this.outputs)
                .properties(this.properties)
                .position(entityPosition)
                .uiMetadata(entityUIMetadata)
                .status(this.status != null ? this.status : Node.NodeStatus.ACTIVE)
                .build();
    }
}