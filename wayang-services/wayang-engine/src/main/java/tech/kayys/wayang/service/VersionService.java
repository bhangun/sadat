package tech.kayys.wayang.service;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.client.VersionServiceClient;
import tech.kayys.wayang.domain.Workflow;

/**
 * VersionService - Local service wrapping version client
 */
@ApplicationScoped
public class VersionService {

    @Inject
    @RestClient
    VersionServiceClient versionClient;

    /**
     * Create version from workflow
     */
    public Uni<VersionServiceClient.WorkflowVersion> createVersion(Workflow workflow) {
        VersionServiceClient.CreateVersionRequest request = new VersionServiceClient.CreateVersionRequest(
                workflow.id.toString(),
                workflow.name,
                workflow.version,
                workflow.tenantId,
                Map.of(
                        "logic", workflow.logic,
                        "ui", workflow.ui,
                        "runtime", workflow.runtime),
                workflow.metadata);

        return versionClient.createVersion(request)
                .onFailure().retry().atMost(2);
    }

    /**
     * Get versions for workflow
     */
    public Uni<List<VersionServiceClient.WorkflowVersion>> getVersions(String workflowId) {
        return versionClient.listVersions(workflowId);
    }

    /**
     * Compare two workflow versions
     */
    public Uni<VersionServiceClient.VersionDiff> compareVersions(String version1, String version2) {
        VersionServiceClient.CompareRequest request = new VersionServiceClient.CompareRequest(version1, version2);
        return versionClient.compareVersions(request);
    }
}