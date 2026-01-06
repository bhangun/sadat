package tech.kayys.wayang.plugin.runtime.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
class OptimizationAction {
    private String type;
    private Object currentValue;
    private Object recommendedValue;
    private String reason;
    private String estimatedImpact;
}
