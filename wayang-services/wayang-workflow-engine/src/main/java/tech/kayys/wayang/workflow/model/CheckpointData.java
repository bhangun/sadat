package tech.kayys.wayang.workflow.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;

/**
 * CheckpointData - Serializable checkpoint state
 * 
 * Contains complete execution state that can be:
 * - Serialized to JSON/JSONB
 * - Stored in database
 * - Used for recovery
 * - Transmitted across services
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckpointData {

    /**
     * Checkpoint metadata
     */
    private String checkpointId;
    private String runId;
    private Integer sequenceNumber;
    private Instant createdAt;
    private String createdBy; // system, manual, auto

    /**
     * Execution state snapshot
     */
    private ExecutionStateSnapshot executionState;

    /**
     * Run metadata at checkpoint time
     */
    private RunMetadata runMetadata;

    /**
     * Workflow definition version at checkpoint
     */
    private String workflowVersion;

    /**
     * Checkpoint options
     */
    private CheckpointOptions options;

    /**
     * Compression info (if compressed)
     */
    private CompressionInfo compressionInfo;

    /**
     * Signature for tamper detection
     */
    private String signature;
}
