package tech.kayys.wayang.workflow.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Checkpoint options
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class CheckpointOptions {
    private boolean includeInputs;
    private boolean includeOutputs;
    private boolean includeVariables;
    private boolean compressData;
    private boolean signCheckpoint;
    private List<String> excludeFields;
}
