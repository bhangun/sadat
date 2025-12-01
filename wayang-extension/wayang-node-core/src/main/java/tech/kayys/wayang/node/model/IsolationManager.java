package tech.kayys.wayang.node.core.isolation;

import tech.kayys.wayang.node.core.model.NodeDescriptor;
import tech.kayys.wayang.node.core.model.NodeContext;
import tech.kayys.wayang.node.core.model.SandboxLevel;
import tech.kayys.wayang.node.core.exception.IsolationException;

/**
 * Manages isolation policies and enforcement for node execution.
 * 
 * Responsibilities:
 * - Determine appropriate isolation level
 * - Apply security policies
 * - Manage namespace boundaries
 * - Coordinate with loader strategies
 */
public interface IsolationManager {
    
    /**
     * Determine the required isolation level for a node
     */
    SandboxLevel determineIsolationLevel(NodeDescriptor descriptor);
    
    /**
     * Apply isolation policies before execution
     */
    void applyIsolation(NodeDescriptor descriptor, NodeContext context) 
        throws IsolationException;
    
    /**
     * Validate isolation constraints
     */
    void validateIsolation(NodeDescriptor descriptor) throws IsolationException;
    
    /**
     * Check if a capability is allowed for this isolation level
     */
    boolean isCapabilityAllowed(String capability, SandboxLevel level);
}