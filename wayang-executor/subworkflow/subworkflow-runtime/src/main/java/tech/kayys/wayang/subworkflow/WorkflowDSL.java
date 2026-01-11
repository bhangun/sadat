package tech.kayys.silat.enhancements;

import java.util.Map;

/**
 * Enhancement: Workflow as Code (YAML/JSON DSL)
 */
record WorkflowDSL(
    String name,
    String version,
    Map<String, Input> inputs,
    Map<String, Output> outputs,
    List<NodeDSL> nodes,
    WorkflowConfig config
) {
    /**
     * Example YAML:
     *
     * name: order-processing
     * version: 1.0.0
     *
     * inputs:
     *   orderId:
     *     type: string
     *     required: true
     *   customerId:
     *     type: string
     *     required: true
     *
     * outputs:
     *   transactionId:
     *     type: string
     *   trackingNumber:
     *     type: string
     *
     * nodes:
     *   - id: validate-order
     *     type: TASK
     *     executor: order-validator
     *     transitions:
     *       - to: fraud-check
     *         condition: success
     *
     *   - id: fraud-check
     *     type: SUB_WORKFLOW
     *     executor: sub-workflow-executor
     *     config:
     *       subWorkflowId: fraud-detection
     *       inputMapping:
     *         customerId: customerId
     *         amount: totalAmount
     *     transitions:
     *       - to: process-payment
     *         condition: fraudApproved == true
     *
     *   - id: process-payment
     *     type: TASK
     *     executor: payment-processor
     *     retry:
     *       maxAttempts: 5
     *       backoffMultiplier: 2.0
     *
     * config:
     *   timeout: 1h
     *   retryPolicy:
     *     maxAttempts: 3
     *   compensationPolicy:
     *     strategy: SEQUENTIAL
     */
}