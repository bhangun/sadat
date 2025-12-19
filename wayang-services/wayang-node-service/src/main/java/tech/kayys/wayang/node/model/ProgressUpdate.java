package tech.kayys.wayang.workflow.model;

/**
 * Progress update DTO
 */
@lombok.Data
@lombok.Builder
public class ProgressUpdate {
    private String currentNodeId;
    private String executedNode;
    private NodeExecutionRecord nodeRecord;
    private Map<String, Object> variables;
    private boolean checkpoint;
}
