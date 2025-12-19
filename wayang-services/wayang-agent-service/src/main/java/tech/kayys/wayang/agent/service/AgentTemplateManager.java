package tech.kayys.wayang.agent.service;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.dto.AgentTemplate;
import tech.kayys.wayang.agent.dto.AgentTemplateDetail;
import tech.kayys.wayang.agent.dto.SaveAsTemplateRequest;
import tech.kayys.wayang.agent.entity.AgentEntity;
import tech.kayys.wayang.agent.entity.TemplateEntity;
import tech.kayys.wayang.agent.repository.TemplateEntityRepository;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class AgentTemplateManager {

    @Inject
    TemplateEntityRepository templateRepository;

    public Uni<List<AgentTemplate>> getTemplates(String category, tech.kayys.wayang.agent.dto.AgentType agentType, String useCase) {
        Log.infof("Getting templates - Category: %s, Type: %s, UseCase: %s", category, agentType, useCase);
        // Return mock templates
        AgentTemplate template1 = new AgentTemplate(
            "template-1",
            "Customer Support Agent",
            "Template for customer support agents",
            "customer-support",
            tech.kayys.wayang.agent.dto.AgentType.AI_AGENT,
            List.of("web-search", "email"),
            Map.of("greeting", "Hello, how can I help you?")
        );
        
        AgentTemplate template2 = new AgentTemplate(
            "template-2", 
            "Data Analyst Agent",
            "Template for data analysis tasks",
            "data-analysis",
            tech.kayys.wayang.agent.dto.AgentType.AI_AGENT,
            List.of("calculator", "database-query"),
            Map.of("analysis_type", "trend-analysis")
        );
        
        return Uni.createFrom().item(List.of(template1, template2));
    }

    public Uni<AgentTemplateDetail> getTemplateDetail(String templateId) {
        Log.infof("Getting template detail: %s", templateId);
        // Return mock template detail
        return Uni.createFrom().item(new AgentTemplateDetail(
            templateId,
            "Mock Template",
            "Detailed description of the mock template",
            "category",
            tech.kayys.wayang.agent.dto.AgentType.AI_AGENT,
            List.of("tool1", "tool2"),
            Map.of("param1", "value1"),
            List.of("example1", "example2")
        ));
    }

    public Uni<AgentTemplate> createTemplate(AgentEntity agent, SaveAsTemplateRequest request) {
        Log.infof("Creating template from agent: %s", agent.getId());
        
        // Save to template entity
        TemplateEntity templateEntity = new TemplateEntity();
        templateEntity.setName(request.name() != null ? request.name() : agent.getName() + " Template");
        templateEntity.setDescription(request.description() != null ? request.description() : agent.getDescription());
        templateEntity.setTemplateType(agent.getType().name());
        templateEntity.setTemplateData(toJsonString(agent.getConfig()));
        // Add other fields as needed
        
        return templateRepository.persist(templateEntity)
            .onItem().transform(entity -> new AgentTemplate(
                entity.getId().toString(),
                entity.getName(),
                entity.getDescription(),
                "default-category",
                agent.getType(),
                List.of("default-tool"),
                Map.of()
            ));
    }

    public Uni<AgentTemplate> getTemplate(String templateId) {
        Log.infof("Getting template: %s", templateId);
        return getTemplateDetail(templateId)
            .onItem().transform(detail -> new AgentTemplate(
                detail.id(),
                detail.name(),
                detail.description(),
                detail.category(),
                detail.agentType(),
                detail.tools(),
                detail.parameters()
            ));
    }

    private String toJsonString(Object obj) {
        return obj != null ? obj.toString() : "{}";
    }
}