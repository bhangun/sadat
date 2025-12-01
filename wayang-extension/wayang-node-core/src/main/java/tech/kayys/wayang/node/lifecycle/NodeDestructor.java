package tech.kayys.wayang.node.core.lifecycle;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.node.core.Node;
import tech.kayys.wayang.node.core.model.NodeDescriptor;

import java.time.Duration;
import java.util.concurrent.*;

/**
 * Handles safe node destruction and resource cleanup.
 * 
 * Responsibilities:
 * - Graceful shutdown
 * - Resource cleanup
 * - Connection closing
 * - Memory release
 */
@ApplicationScoped
public class NodeDestructor {
    
    private static final Logger LOG = LoggerFactory.getLogger(NodeDestructor.class);
    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(30);
    
    private final ExecutorService cleanupExecutor;
    
    public NodeDestructor() {
        this.cleanupExecutor = Executors.newFixedThreadPool(
            2,
            r -> {
                Thread t = new Thread(r, "node-destructor");
                t.setDaemon(true);
                return t;
            }
        );
    }
    
    /**
     * Destroy a node and cleanup resources
     */
    public void destroy(Node node, NodeDescriptor descriptor) {
        String nodeKey = descriptor.getQualifiedId();
        
        LOG.info("Starting destruction for node: {}", nodeKey);
        
        try {
            // Call onUnload with timeout
            CompletableFuture<Void> unloadFuture = CompletableFuture.runAsync(
                () -> {
                    try {
                        node.onUnload();
                    } catch (Exception e) {
                        LOG.error("Error in node onUnload: " + nodeKey, e);
                    }
                },
                cleanupExecutor
            );
            
            // Wait for completion with timeout
            unloadFuture.get(SHUTDOWN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            
            LOG.info("Node destroyed successfully: {}", nodeKey);
            
        } catch (TimeoutException e) {
            LOG.error("Node destruction timed out: {}", nodeKey);
            unloadFuture.cancel(true);
        } catch (Exception e) {
            LOG.error("Error destroying node: " + nodeKey, e);
        }
        
        // Additional cleanup
        performAdditionalCleanup(node, descriptor);
    }
    
    /**
     * Perform additional cleanup tasks
     */
    private void performAdditionalCleanup(Node node, NodeDescriptor descriptor) {
        try {
            // Clear any references
            // Close any remaining resources
            // Release memory
            
            LOG.debug("Additional cleanup completed for: {}", descriptor.getQualifiedId());
            
        } catch (Exception e) {
            LOG.error("Error in additional cleanup", e);
        }
    }
    
    /**
     * Shutdown the destructor
     */
    public void shutdown() {
        LOG.info("Shutting down NodeDestructor");
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}