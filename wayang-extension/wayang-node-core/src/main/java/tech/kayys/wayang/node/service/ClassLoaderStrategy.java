package tech.kayys.wayang.node.core.loader;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.node.core.Node;
import tech.kayys.wayang.node.core.exception.NodeLoadException;
import tech.kayys.wayang.node.core.isolation.ResourceQuotaController;
import tech.kayys.wayang.node.core.model.*;

import java.util.concurrent.CompletionStage;

/**
 * Loader strategy using ClassLoader isolation.
 * 
 * For TRUSTED nodes: minimal overhead, standard classloader
 * For SEMI_TRUSTED nodes: isolated classloader + resource monitoring
 */
@ApplicationScoped
public class ClassLoaderStrategy implements NodeLoaderStrategy {
    
    private static final Logger LOG = LoggerFactory.getLogger(ClassLoaderStrategy.class);
    
    private final ResourceQuotaController quotaController;
    private final Tracer tracer;
    
    @Inject
    public ClassLoaderStrategy(
        ResourceQuotaController quotaController,
        Tracer tracer
    ) {
        this.quotaController = quotaController;
        this.tracer = tracer;
    }
    
    @Override
    public Node load(NodeDescriptor descriptor, Node nodeInstance) 
            throws NodeLoadException {
        
        if (descriptor.sandboxLevel() == SandboxLevel.SEMI_TRUSTED) {
            // Wrap with monitoring and quota enforcement
            return new MonitoredNode(nodeInstance, descriptor, quotaController, tracer);
        }
        
        // Trusted nodes run without additional wrapping
        return nodeInstance;
    }
    
    @Override
    public void unload(NodeDescriptor descriptor, Node node) {
        // Cleanup handled by node's onUnload()
        try {
            node.onUnload();
        } catch (Exception e) {
            LOG.error("Error during node unload: " + descriptor.getQualifiedId(), e);
        }
    }
    
    @Override
    public boolean supports(SandboxLevel sandboxLevel) {
        return sandboxLevel == SandboxLevel.TRUSTED || 
               sandboxLevel == SandboxLevel.SEMI_TRUSTED;
    }
    
    @Override
    public SandboxLevel getSandboxLevel() {
        return SandboxLevel.TRUSTED; // Primary support
    }
    
    /**
     * Wrapper node that adds monitoring and resource quota enforcement
     */
    private static class MonitoredNode implements Node {
        
        private final Node delegate;
        private final NodeDescriptor descriptor;
        private final ResourceQuotaController quotaController;
        private final Tracer tracer;
        
        MonitoredNode(
            Node delegate, 
            NodeDescriptor descriptor,
            ResourceQuotaController quotaController,
            Tracer tracer
        ) {
            this.delegate = delegate;
            this.descriptor = descriptor;
            this.quotaController = quotaController;
            this.tracer = tracer;
        }
        
        @Override
        public void onLoad(NodeDescriptor descriptor, NodeConfig config) 
                throws tech.kayys.wayang.node.core.exception.NodeException {
            delegate.onLoad(descriptor, config);
        }
        
        @Override
        public CompletionStage<ExecutionResult> execute(NodeContext context) 
                throws tech.kayys.wayang.node.core.exception.NodeException {
            
            Span span = tracer.spanBuilder("node.execute.monitored")
                .setAttribute("node.id", descriptor.id())
                .setAttribute("node.version", descriptor.version())
                .startSpan();
            
            try {
                // Check resource quota before execution
                quotaController.checkQuota(descriptor, context);
                
                // Execute with monitoring
                return delegate.execute(context)
                    .whenComplete((result, throwable) -> {
                        if (throwable == null) {
                            // Record resource usage
                            quotaController.recordUsage(
                                descriptor, 
                                context, 
                                result.metrics()
                            );
                            span.addEvent("execution.success");
                        } else {
                            span.recordException(throwable);
                        }
                        span.end();
                    });
                    
            } catch (Exception e) {
                span.recordException(e);
                span.end();
                throw e;
            }
        }
        
        @Override
        public void onUnload() {
            delegate.onUnload();
        }
        
        @Override
        public NodeDescriptor getDescriptor() {
            return descriptor;
        }
        
        @Override
        public boolean supportsStreaming() {
            return delegate.supportsStreaming();
        }
        
        @Override
        public boolean supportsCheckpointing() {
            return delegate.supportsCheckpointing();
        }
        
        @Override
        public void validateInputs(NodeContext context) 
                throws tech.kayys.wayang.node.core.exception.NodeException {
            delegate.validateInputs(context);
        }
    }
}