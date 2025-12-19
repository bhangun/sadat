package tech.kayys.wayang.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.domain.Workflow;
import tech.kayys.wayang.schema.WorkflowDTO;

@ApplicationScoped
public class WorkflowMapper {

    public WorkflowDTO toDTO(Workflow workflow) {
        if (workflow == null)
            return null;
        WorkflowDTO dto = new WorkflowDTO();
        dto.setId(workflow.id != null ? workflow.id.toString() : null);
        // Map other fields
        return dto;
    }

    public tech.kayys.wayang.model.LogicDefinition toLogicEntity(tech.kayys.wayang.schema.LogicDefinitionDTO dto) {
        return dto == null ? null : new tech.kayys.wayang.model.LogicDefinition();
    }

    public tech.kayys.wayang.model.UIDefinition toUIEntity(tech.kayys.wayang.schema.UIDefinitionDTO dto) {
        return dto == null ? null : new tech.kayys.wayang.model.UIDefinition();
    }

    public tech.kayys.wayang.model.RuntimeConfig toRuntimeEntity(tech.kayys.wayang.schema.RuntimeConfigDTO dto) {
        return dto == null ? null : new tech.kayys.wayang.model.RuntimeConfig();
    }

    public tech.kayys.wayang.schema.NodeDTO toNodeDTO(tech.kayys.wayang.model.NodeDefinition node) {
        if (node == null)
            return null;
        tech.kayys.wayang.schema.NodeDTO dto = new tech.kayys.wayang.schema.NodeDTO();
        dto.setId(node.id);
        dto.setName(node.name);
        return dto;
    }

    public java.util.Map<String, Object> buildChangeSet(Workflow workflow,
            tech.kayys.wayang.schema.UpdateWorkflowInput input) {
        return java.util.Collections.emptyMap();
    }
}
