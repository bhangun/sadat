package tech.kayys.wayang.mcp.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.mcp.domain.*;
import tech.kayys.wayang.mcp.runtime.*;

/**
 * Tool metrics collector
 */
@ApplicationScoped
public class ToolMetricsCollector {

    public Uni<Void> collect(String toolId, ToolExecutionResult result) {
        // Update tool metrics asynchronously
        return Uni.createFrom().voidItem();
    }
}