package tech.kayys.wayang.workflow.version.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.workflow.version.dto.SemanticVersion;
import tech.kayys.wayang.workflow.version.model.WorkflowVersion;

import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * VersionStore: Persistence layer for workflow versions.
 * 
 * Implementation Notes:
 * - In-memory store for demonstration
 * - Production: Use PostgreSQL with Hibernate Reactive
 * - Stores workflow definitions as JSONB
 * - Indexes on: workflowId, version, status
 */
@ApplicationScoped
public class VersionStore {

    private static final Logger LOG = Logger.getLogger(VersionStore.class);

    private final Map<String, WorkflowVersion> versions = new ConcurrentHashMap<>();

    /**
     * Save or update a version.
     */
    public Uni<WorkflowVersion> save(WorkflowVersion version) {
        versions.put(version.getVersionId(), version);
        LOG.debugf("Saved version %s", version.getVersionId());
        return Uni.createFrom().item(version);
    }

    /**
     * Find version by ID.
     */
    public Uni<WorkflowVersion> findById(String versionId) {
        WorkflowVersion version = versions.get(versionId);
        return Uni.createFrom().item(version);
    }

    /**
     * Find all versions for a workflow.
     */
    public Uni<List<WorkflowVersion>> findByWorkflowId(String workflowId) {
        List<WorkflowVersion> result = versions.values().stream()
                .filter(v -> v.getWorkflowId().equals(workflowId))
                .collect(Collectors.toList());

        return Uni.createFrom().item(result);
    }

    /**
     * Find specific workflow version.
     */
    public Uni<WorkflowVersion> findByWorkflowAndVersion(
            String workflowId,
            String version) {

        WorkflowVersion result = versions.values().stream()
                .filter(v -> v.getWorkflowId().equals(workflowId))
                .filter(v -> v.getVersion().equals(version))
                .findFirst()
                .orElse(null);

        return Uni.createFrom().item(result);
    }

    /**
     * Find latest version for a workflow.
     */
    public Uni<WorkflowVersion> findLatestVersion(String workflowId) {
        WorkflowVersion latest = versions.values().stream()
                .filter(v -> v.getWorkflowId().equals(workflowId))
                .max((v1, v2) -> {
                    SemanticVersion sv1 = SemanticVersion.parse(v1.getVersion());
                    SemanticVersion sv2 = SemanticVersion.parse(v2.getVersion());
                    return sv1.compareTo(sv2);
                })
                .orElse(null);

        return Uni.createFrom().item(latest);
    }
}
