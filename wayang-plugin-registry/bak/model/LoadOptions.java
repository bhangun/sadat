package tech.kayys.wayang.plugin.runtime.model;

import lombok.Builder;
import lombok.Data;

/**
 * Load Options
 */
@Data
@Builder
class LoadOptions {
    private boolean forceReload;
    private int timeout;
    private String tenantId;
}