package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Serverless Workflow Execution
 */
interface ServerlessWorkflowService {

    /**
     * Execute workflow on AWS Lambda
     */
    Uni<tech.kayys.silat.core.domain.WorkflowRun> executeOnLambda(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        Map<String, Object> inputs,
        LambdaConfig config
    );

    /**
     * Execute on Google Cloud Functions
     */
    Uni<tech.kayys.silat.core.domain.WorkflowRun> executeOnGCF(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        Map<String, Object> inputs,
        GCFConfig config
    );

    /**
     * Execute on Azure Functions
     */
    Uni<tech.kayys.silat.core.domain.WorkflowRun> executeOnAzure(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        Map<String, Object> inputs,
        AzureConfig config
    );
}