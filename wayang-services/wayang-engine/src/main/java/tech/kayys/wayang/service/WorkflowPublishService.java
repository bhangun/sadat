package tech.kayys.wayang.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.schema.PublishRequest;
import tech.kayys.wayang.tenant.TenantContext;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for workflow publishing operations
 */
@ApplicationScoped
public class WorkflowPublishService {

    private static final Logger LOG = Logger.getLogger(WorkflowPublishService.class);

    @Inject
    TenantContext tenantContext;

    @Inject
    WorkflowService workflowService;

    @Inject
    ValidationService validationService;

    /**
     * Publish workflow to production
     * 
     * @param workflowId Workflow ID
     * @param request    Publish request with version and description
     * @param tenantId   Tenant ID
     * @param userId     User ID
     * @return Publish result
     */
    public Uni<PublishResult> publish(String workflowId, PublishRequest request, String tenantId, String userId) {
        LOG.infof("Publishing workflow %s (version=%s, tenant=%s, user=%s)",
                workflowId, request.getVersion(), tenantId, userId);

        UUID wfId = UUID.fromString(workflowId);

        // Validate workflow before publishing
        return workflowService.getWorkflow(wfId)
                .flatMap(workflow -> {
                    // Validate the workflow
                    return validationService.validateWorkflow(workflow)
                            .flatMap(validationResult -> {
                                if (!validationResult.isValid()) {
                                    return Uni.createFrom().failure(
                                            new IllegalStateException("Cannot publish invalid workflow"));
                                }

                                // Publish the workflow
                                return workflowService.publishWorkflow(wfId)
                                        .map(published -> {
                                            PublishResult result = new PublishResult();
                                            result.setWorkflowId(workflowId);
                                            result.setVersion(request.getVersion() != null ? request.getVersion()
                                                    : published.version);
                                            result.setPublishedAt(Instant.now());
                                            result.setActive(request.isActive());
                                            result.setSuccess(true);
                                            return result;
                                        });
                            });
                });
    }

    /**
     * Publish result
     */
    public static class PublishResult {
        private String workflowId;
        private String version;
        private Instant publishedAt;
        private boolean active;
        private boolean success;
        private String message;

        public String getWorkflowId() {
            return workflowId;
        }

        public void setWorkflowId(String workflowId) {
            this.workflowId = workflowId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public Instant getPublishedAt() {
            return publishedAt;
        }

        public void setPublishedAt(Instant publishedAt) {
            this.publishedAt = publishedAt;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
