package tech.kayys.silat.persistence.subworkflow;

enum SubWorkflowExecutionMode {
    SYNCHRONOUS,    // Wait for completion
    ASYNCHRONOUS,   // Fire and forget
    DETACHED        // Detached execution
}