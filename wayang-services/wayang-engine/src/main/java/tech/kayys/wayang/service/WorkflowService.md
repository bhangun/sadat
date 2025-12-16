package tech.kayys.wayang.service;

import io.smallrye.mutiny.Uni;
import io.smallrye.openapi.runtime.scanner.SchemaRegistry;
import io.quarkus.hibernate.reactive.panache.Panache;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.NotFoundException;
import tech.kayys.wayang.domain.Workflow;
import tech.kayys.wayang.domain.Workflow.WorkflowStatus;
import tech.kayys.wayang.model.ValidationResult;
import tech.kayys.wayang.model.WorkflowDefinition;
import tech.kayys.wayang.repository.WorkflowRepository;

import org.jboss.logging.Logger;

import com.arjuna.ats.txoj.LockManager;

import java.util.ArrayList;
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

    @Inject
    WorkflowRepository workflowRepository;

    @Inject
    VersioningService versioningService;
    @Inject
    AutosaveManager autosaveManager;
    @Inject
    LockManager lockManager;

    public Uni<WorkflowDefinition> createWorkflow(
            String tenantId,
            String userId,
            WorkflowDefinition workflow) {
        return validator.validate(workflow)
                .flatMap(validationResult -> {
                    if (!validationResult.isValid()) {
                        return Uni.createFrom().failure(
                                new ValidationException("Workflow validation failed",
                                        validationResult.errors()));
                    }

                    workflow.setId(UUID.randomUUID().toString());
                    workflow.setTenantId(tenantId);
                    workflow.setCreatedBy(userId);
                    workflow.setStatus(WorkflowStatus.DRAFT);

                    return Panache.withTransaction(() -> repository.persist(workflow));
                })
                .invoke(() -> LOG.infof("Workflow created: %s", workflow.getId()));
    }

    public Uni<ValidationResult> validateWorkflow(String tenantId, String workflowId) {
        return repository.findByIdAndTenant(workflowId, tenantId)
                .flatMap(workflow -> {
                    if (workflow == null) {
                        return Uni.createFrom().failure(
                                new NotFoundException("Workflow not found"));
                    }
                    return validator.validate(workflow);
                });
    }

    public Uni<WorkflowDefinition> publishWorkflow(
            String tenantId,
            String workflowId,
            String version) {
        return validateWorkflow(tenantId, workflowId)
                .flatMap(validationResult -> {
                    if (!validationResult.isValid()) {
                        return Uni.createFrom().failure(
                                new ValidationException("Cannot publish invalid workflow"));
                    }

                    return Panache.withTransaction(() -> repository.findByIdAndTenant(workflowId, tenantId)
                            .flatMap(workflow -> {
                                workflow.setStatus(WorkflowStatus.PUBLISHED);
                                workflow.setVersion(version);
                                return repository.persist(workflow);
                            }));
                });
    }

    @Override
    @Transactional
    public Workflow createWorkflow(CreateWorkflowRequest request) {
        // Validate request
        validateCreateRequest(request);

        // Create workflow
        Workflow workflow = Workflow.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .version("1.0.0")
                .definition(WorkflowDefinition.builder()
                        .nodes(new ArrayList<>())
                        .edges(new ArrayList<>())
                        .globalVariables(request.getGlobalVariables())
                        .build())
                .status(WorkflowStatus.DRAFT)
                .tenantId(request.getTenantId())
                .createdBy(request.getUserId())
                .build();

        // Persist
        workflow = workflowRepository.save(workflow);

        // Enable autosave
        autosaveManager.enableAutosave(workflow.getId());

        return workflow;
    }

    @Override
    @Transactional
    public Workflow updateWorkflow(UUID workflowId, UpdateWorkflowRequest request) {
        // Acquire lock
        Lock lock = lockManager.acquireLock(workflowId, request.getUserId());

        try {
            // Get workflow
            Workflow workflow = workflowRepository.findById(workflowId)
                    .orElseThrow(() -> new WorkflowNotFoundException(workflowId));

            // Check if editable
            if (workflow.getStatus() == WorkflowStatus.PUBLISHED) {
                throw new WorkflowNotEditableException(workflowId);
            }

            // Apply updates
            if (request.getName() != null) {
                workflow.setName(request.getName());
            }

            if (request.getDefinition() != null) {
                workflow.setDefinition(request.getDefinition());
            }

            // Validate
            ValidationResult validation = workflowValidator.validate(workflow);
            if (!validation.isValid()) {
                workflow.setStatus(WorkflowStatus.INVALID);
            } else {
                workflow.setStatus(WorkflowStatus.VALID);
            }

            // Save
            workflow = workflowRepository.save(workflow);

            // Autosave
            autosaveManager.save(workflow);

            return workflow;

        } finally {
            lock.release();
        }
    }

    @Override
    @Transactional
    public Workflow publishWorkflow(UUID workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowId));

        // Validate
        ValidationResult validation = workflowValidator.validate(workflow);
        if (!validation.isValid()) {
            throw new InvalidWorkflowException(validation.getErrors());
        }

        // Create version
        WorkflowVersion version = versioningService.createVersion(workflow);

        // Update status
        workflow.setStatus(WorkflowStatus.PUBLISHED);
        workflow.setVersion(version.getVersion());

        return workflowRepository.save(workflow);
    }
}