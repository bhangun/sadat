package tech.kayys.wayang.node.core;

import tech.kayys.wayang.node.core.model.ExecutionResult;
import tech.kayys.wayang.node.core.model.NodeContext;
import tech.kayys.wayang.node.core.model.NodeDescriptor;
import tech.kayys.wayang.node.core.exception.NodeException;

import java.util.concurrent.CompletionStage;

/**
 * Core interface that all executable nodes must implement.
 * 
 * Nodes are the fundamental unit of execution in the Wayang platform.
 * They can be synchronous or asynchronous, stateless or stateful.
 * 
 * Implementations must be thread-safe if they maintain internal state.
 * 
 * Design principles:
 * - Single responsibility
 * - Declarative configuration
 * - Observable execution
 * - Composable behavior
 * 
 * Lifecycle:
 * 1. onLoad() - called once when node is loaded
 * 2. execute() - called for each execution request
 * 3. onUnload() - called before node is unloaded
 */
public interface Node {
    /**
     * Unique node identifier
     */
    String getNodeId();
    
    /**
     * Initialize the node with its descriptor and configuration.
     * 
     * This method is called once when the node is loaded into the runtime.
     * Use this for:
     * - Validating configuration
     * - Initializing resources (connections, caches, etc.)
     * - Setting up internal state
     * 
     * @param descriptor The node descriptor containing metadata and schema
     * @param config Configuration properties specific to this node instance
     * @throws NodeException if initialization fails
     */
    void onLoad(NodeDescriptor descriptor, NodeConfig config) throws NodeException;
    
    /**
     * Execute the node's logic with the given context.
     * 
     * This method contains the core business logic of the node.
     * It should be idempotent when possible to support retries.
     * 
     * For long-running operations, consider:
     * - Implementing checkpointing
     * - Honoring context deadline
     * - Emitting progress events
     * 
     * @param context The execution context containing inputs, services, and metadata
     * @return CompletionStage with the execution result
     * @throws NodeException if execution fails unrecoverably
     */
    CompletionStage<ExecutionResult> execute(NodeContext context) throws NodeException;
    
    /**
     * Clean up resources before the node is unloaded.
     * 
     * This method is called once when the node is being removed from the runtime.
     * Use this for:
     * - Closing connections
     * - Flushing caches
     * - Releasing resources
     * 
     * Implementations should not throw exceptions.
     */
    void onUnload();
    
    /**
     * Get the descriptor for this node.
     * Node descriptor containing metadata and schema
     * 
     * @return The node descriptor
     */
    NodeDescriptor getDescriptor();
    
    /**
     * Check if this node supports streaming execution.
     * 
     * @return true if the node can stream partial results
     */
    default boolean supportsStreaming() {
        return false;
    }
    
    /**
     * Check if this node supports checkpointing for long-running operations.
     * 
     * @return true if the node can create checkpoints
     */
    default boolean supportsCheckpointing() {
        return false;
    }
    
    /**
     * Validate inputs before execution.
     * 
     * This is called before execute() to fail fast on invalid inputs.
     * 
     * @param context The execution context
     * @throws NodeException if validation fails
     */
    default void validateInputs(NodeContext context) throws NodeException {
        // Default implementation does nothing
    }


    /**
     * Validate node configuration and inputs
     */
    ValidationResult validate(NodeContext context);
}

