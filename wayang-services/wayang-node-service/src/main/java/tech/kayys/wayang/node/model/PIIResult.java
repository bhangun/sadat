package tech.kayys.wayang.workflow.model;

import java.util.Set;

@lombok.Data
@lombok.Builder
class PIIResult {
    private final boolean hasPII;
    private final Set<String> types;
}
