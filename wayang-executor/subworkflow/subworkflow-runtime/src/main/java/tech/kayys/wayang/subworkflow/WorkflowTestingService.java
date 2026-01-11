package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Workflow Testing Framework
 */
interface WorkflowTestingService {

    /**
     * Create test case for workflow
     */
    Uni<WorkflowTestCase> createTestCase(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        String name,
        Map<String, Object> inputs,
        Map<String, Object> expectedOutputs,
        List<NodeAssertion> nodeAssertions
    );

    /**
     * Run test suite
     */
    Uni<TestSuiteResult> runTestSuite(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        List<String> testCaseIds
    );

    /**
     * Generate test cases from execution history
     */
    Uni<List<WorkflowTestCase>> generateTestsFromHistory(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        int sampleSize
    );

    /**
     * Mock external dependencies
     */
    Uni<Void> createMock(
        String nodeId,
        MockBehavior behavior
    );
}