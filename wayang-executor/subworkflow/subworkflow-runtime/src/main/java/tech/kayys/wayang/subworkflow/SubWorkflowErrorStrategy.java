package tech.kayys.silat.executor.subworkflow;

/**
 * Error handling strategies
 */
enum SubWorkflowErrorStrategy {
    PROPAGATE,              // Propagate error to parent (fail parent node)
    IGNORE,                 // Ignore error (parent node succeeds)
    RETRY_SUB_WORKFLOW,     // Retry the sub-workflow
    CUSTOM                  // Custom error handling
}