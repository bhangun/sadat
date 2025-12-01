package tech.kayys.wayang.designer.service;

import tech.kayys.wayang.common.domain.*;
import tech.kayys.wayang.designer.repository.WorkflowRepository;
import tech.kayys.wayang.designer.validator.WorkflowValidator;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.Panache;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
public class WorkflowService {
    
    private static final Logger LOG = Logger.getLogger(WorkflowService.class);
    
    @Inject
    WorkflowRepository repository;
    
    @Inject
    WorkflowValidator validator;
    
    @Inject
    SchemaRegistryService schemaRegistry;
    
    public Uni<WorkflowDefinition> createWorkflow(
        String tenantId, 
        String userId, 
        WorkflowDefinition workflow
    ) {
        return validator.validate(workflow)
            .flatMap(validationResult -> {
                if (!validationResult.isValid()) {
                    return Uni.createFrom().failure(
                        new ValidationException("Workflow validation failed", 
                                              validationResult.errors())
                    );
                }
                
                workflow.setId(UUID.randomUUID().toString());
                workflow.setTenantId(tenantId);
                workflow.setCreatedBy(userId);
                workflow.setStatus(WorkflowStatus.DRAFT);
                
                return Panache.withTransaction(() -> 
                    repository.persist(workflow)
                );
            })
            .invoke(() -> LOG.infof("Workflow created: %s", workflow.getId()));
    }
    
    public Uni<ValidationResult> validateWorkflow(String tenantId, String workflowId) {
        return repository.findByIdAndTenant(workflowId, tenantId)
            .flatMap(workflow -> {
                if (workflow == null) {
                    return Uni.createFrom().failure(
                        new NotFoundException("Workflow not found")
                    );
                }
                return validator.validate(workflow);
            });
    }
    
    public Uni<WorkflowDefinition> publishWorkflow(
        String tenantId, 
        String workflowId, 
        String version
    ) {
        return validateWorkflow(tenantId, workflowId)
            .flatMap(validationResult -> {
                if (!validationResult.isValid()) {
                    return Uni.createFrom().failure(
                        new ValidationException("Cannot publish invalid workflow")
                    );
                }
                
                return Panache.withTransaction(() -> 
                    repository.findByIdAndTenant(workflowId, tenantId)
                        .flatMap(workflow -> {
                            workflow.setStatus(WorkflowStatus.PUBLISHED);
                            workflow.setVersion(version);
                            return repository.persist(workflow);
                        })
                );
            });
    }
}