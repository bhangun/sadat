
public interface VersioningService {
    WorkflowVersion createVersion(Workflow workflow);
    WorkflowVersion getVersion(UUID workflowId, String version);
    List<WorkflowVersion> listVersions(UUID workflowId);
    WorkflowVersion tagVersion(UUID workflowId, String version, String tag);
    Workflow restoreVersion(UUID workflowId, String version);
    ComparisonResult compare(UUID workflowId, String version1, String version2);
}
