package tech.kayys.wayang.common.spi;

import tech.kayys.wayang.workflow.api.dto.AuditPayload;

public interface ProvenanceContext {
    void log(AuditPayload payload);
}
