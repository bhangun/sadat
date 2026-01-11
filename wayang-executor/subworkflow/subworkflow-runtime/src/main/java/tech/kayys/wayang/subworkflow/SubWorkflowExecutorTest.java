package tech.kayys.silat.executor.subworkflow;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;
import tech.kayys.silat.client.SilatClient;
import tech.kayys.silat.core.domain.*;
import tech.kayys.silat.api.dto.*;

import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sub-workflow executor tests
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SubWorkflowExecutorTest {

    @Inject
    SubWorkflowExecutor subWorkflowExecutor;

    @Inject
    SubWorkflowRelationshipService relationshipService;

    private SilatClient client;

    @BeforeEach
    void setup() {
        client = SilatClient.builder()
            .restEndpoint("http://localhost:8080")
            .tenantId("test-tenant")
            .apiKey("test-key")
            .build();
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    // ==================== BASIC TESTS ====================

    @Test
    @Order(1)
    @DisplayName("Test basic sub-workflow execution")
    void testBasicSubWorkflowExecution() {
        // Create child workflow
        WorkflowDefinitionResponse childWorkflow = createSimpleChildWorkflow();

        // Create parent workflow with sub-workflow node
        WorkflowDefinitionResponse parentWorkflow = client.workflows()
            .create("parent-workflow")
            .version("1.0.0")
            .addNode(createSubWorkflowNode(
                "call-child",
                childWorkflow.definitionId(),
                Map.of("input1", "parentInput1"),
                Map.of("parentOutput1", "output1")
            ))
            .addInput("parentInput1", stringInput("Parent input", true))
            .addOutput("parentOutput1", stringOutput("Parent output"))
            .execute()
            .await().atMost(Duration.ofSeconds(10));

        // Execute parent workflow
        RunResponse run = client.runs()
            .create(parentWorkflow.definitionId())
            .input("parentInput1", "test-value")
            .executeAndStart()
            .await().atMost(Duration.ofSeconds(10));

        // Wait for completion
        RunResponse completed = waitForCompletion(run.runId());

        assertEquals("COMPLETED", completed.status());
        assertNotNull(completed.variables().get("parentOutput1"));

        // Verify relationship was created
        List<ChildWorkflowInfo> children = relationshipService
            .getChildWorkflows(
                WorkflowRunId.of(run.runId()),
                TenantId.of("test-tenant"))
            .await().atMost(Duration.ofSeconds(5));

        assertEquals(1, children.size());
    }

    @Test
    @Order(2)
    @DisplayName("Test input/output mapping")
    void testInputOutputMapping() {
        WorkflowDefinitionResponse childWorkflow = createMappingTestWorkflow();

        WorkflowDefinitionResponse parentWorkflow = client.workflows()
            .create("mapping-parent")
            .version("1.0.0")
            .addNode(createSubWorkflowNode(
                "mapped-child",
                childWorkflow.definitionId(),
                Map.of(
                    "childField1", "parentField1",
                    "childField2", "parentField2"
                ),
                Map.of(
                    "parentResult1", "childResult1",
                    "parentResult2", "childResult2"
                )
            ))
            .addInput("parentField1", stringInput("Field 1", true))
            .addInput("parentField2", stringInput("Field 2", true))
            .addOutput("parentResult1", stringOutput("Result 1"))
            .addOutput("parentResult2", stringOutput("Result 2"))
            .execute()
            .await().atMost(Duration.ofSeconds(10));

        RunResponse run = client.runs()
            .create(parentWorkflow.definitionId())
            .input("parentField1", "value1")
            .input("parentField2", "value2")
            .executeAndStart()
            .await().atMost(Duration.ofSeconds(10));

        RunResponse completed = waitForCompletion(run.runId());

        assertEquals("COMPLETED", completed.status());
        assertEquals("transformed-value1", completed.variables().get("parentResult1"));
        assertEquals("transformed-value2", completed.variables().get("parentResult2"));
    }

    @Test
    @Order(3)
    @DisplayName("Test error propagation")
    void testErrorPropagation() {
        WorkflowDefinitionResponse failingChild = createFailingWorkflow();

        WorkflowDefinitionResponse parentWorkflow = client.workflows()
            .create("error-parent")
            .version("1.0.0")
            .addNode(createSubWorkflowNode(
                "failing-child",
                failingChild.definitionId(),
                Map.of(),
                Map.of(),
                "PROPAGATE" // Error strategy
            ))
            .execute()
            .await().atMost(Duration.ofSeconds(10));

        RunResponse run = client.runs()
            .create(parentWorkflow.definitionId())
            .executeAndStart()
            .await().atMost(Duration.ofSeconds(10));

        RunResponse completed = waitForCompletion(run.runId());

        assertEquals("FAILED", completed.status());
        assertTrue(completed.nodeExecutions().get("failing-child")
            .error().message().contains("Sub-workflow execution failed"));
    }

    @Test
    @Order(4)
    @DisplayName("Test error ignore strategy")
    void testErrorIgnoreStrategy() {
        WorkflowDefinitionResponse failingChild = createFailingWorkflow();

        WorkflowDefinitionResponse parentWorkflow = client.workflows()
            .create("ignore-error-parent")
            .version("1.0.0")
            .addNode(createSubWorkflowNode(
                "failing-child-ignored",
                failingChild.definitionId(),
                Map.of(),
                Map.of(),
                "IGNORE" // Error strategy
            ))
            .execute()
            .await().atMost(Duration.ofSeconds(10));

        RunResponse run = client.runs()
            .create(parentWorkflow.definitionId())
            .executeAndStart()
            .await().atMost(Duration.ofSeconds(10));

        RunResponse completed = waitForCompletion(run.runId());

        // Parent should succeed despite child failure
        assertEquals("COMPLETED", completed.status());
        assertTrue((Boolean) completed.nodeExecutions()
            .get("failing-child-ignored")
            .output().get("errorIgnored"));
    }

    @Test
    @Order(5)
    @DisplayName("Test sub-workflow timeout")
    void testSubWorkflowTimeout() {
        WorkflowDefinitionResponse slowChild = createSlowWorkflow(Duration.ofSeconds(60));

        WorkflowDefinitionResponse parentWorkflow = client.workflows()
            .create("timeout-parent")
            .version("1.0.0")
            .addNode(createSubWorkflowNodeWithTimeout(
                "slow-child",
                slowChild.definitionId(),
                5 // 5 second timeout
            ))
            .execute()
            .await().atMost(Duration.ofSeconds(10));

        RunResponse run = client.runs()
            .create(parentWorkflow.definitionId())
            .executeAndStart()
            .await().atMost(Duration.ofSeconds(10));

        RunResponse completed = waitForCompletion(run.runId());

        assertEquals("FAILED", completed.status());
        assertTrue(completed.nodeExecutions().get("slow-child")
            .error().code().equals("SUB_WORKFLOW_TIMEOUT"));
    }

    // ==================== ADVANCED TESTS ====================

    @Test
    @Order(6)
    @DisplayName("Test nested sub-workflows (3 levels)")
    void testNestedSubWorkflows() {
        // Level 3 (innermost)
        WorkflowDefinitionResponse level3 = createSimpleChildWorkflow("level-3");

        // Level 2 (calls level 3)
        WorkflowDefinitionResponse level2 = client.workflows()
            .create("level-2")
            .version("1.0.0")
            .addNode(createSubWorkflowNode("call-level-3", level3.definitionId()))
            .execute()
            .await().atMost(Duration.ofSeconds(10));

        // Level 1 (calls level 2)
        WorkflowDefinitionResponse level1 = client.workflows()
            .create("level-1")
            .version("1.0.0")
            .addNode(createSubWorkflowNode("call-level-2", level2.definitionId()))
            .execute()
            .await().atMost(Duration.ofSeconds(10));

        // Execute root
        RunResponse run = client.runs()
            .create(level1.definitionId())
            .executeAndStart()
            .await().atMost(Duration.ofSeconds(10));

        RunResponse completed = waitForCompletion(run.runId());

        assertEquals("COMPLETED", completed.status());

        // Verify hierarchy
        WorkflowHierarchy hierarchy = client.workflows()
            .getHierarchy(run.runId())
            .await().atMost(Duration.ofSeconds(5));

        assertEquals(3, hierarchy.totalNodes());
        assertEquals(2, hierarchy.maxDepth());
    }

    @Test
    @Order(7)
    @DisplayName("Test parallel sub-workflows")
    void testParallelSubWorkflows() {
        WorkflowDefinitionResponse childA = createSimpleChildWorkflow("child-a");
        WorkflowDefinitionResponse childB = createSimpleChildWorkflow("child-b");
        WorkflowDefinitionResponse childC = createSimpleChildWorkflow("child-c");

        WorkflowDefinitionResponse parentWorkflow = client.workflows()
            .create("parallel-parent")
            .version("1.0.0")
            .addNode(createSubWorkflowNode("child-a-call", childA.definitionId()))
            .addNode(createSubWorkflowNode("child-b-call", childB.definitionId()))
            .addNode(createSubWorkflowNode("child-c-call", childC.definitionId()))
            .execute()
            .await().atMost(Duration.ofSeconds(10));

        RunResponse run = client.runs()
            .create(parentWorkflow.definitionId())
            .executeAndStart()
            .await().atMost(Duration.ofSeconds(10));

        RunResponse completed = waitForCompletion(run.runId());

        assertEquals("COMPLETED", completed.status());

        // Verify all children executed
        List<ChildWorkflowInfo> children = relationshipService
            .getChildWorkflows(
                WorkflowRunId.of(run.runId()),
                TenantId.of("test-tenant"))
            .await().atMost(Duration.ofSeconds(5));

        assertEquals(3, children.size());
    }

    @Test
    @Order(8)
    @DisplayName("Test fire-and-forget mode")
    void testFireAndForget() {
        WorkflowDefinitionResponse childWorkflow = createSlowWorkflow(Duration.ofSeconds(30));

        WorkflowDefinitionResponse parentWorkflow = client.workflows()
            .create("fire-forget-parent")
            .version("1.0.0")
            .addNode(createFireAndForgetNode("async-child", childWorkflow.definitionId()))
            .execute()
            .await().atMost(Duration.ofSeconds(10));

        long startTime = System.currentTimeMillis();

        RunResponse run = client.runs()
            .create(parentWorkflow.definitionId())
            .executeAndStart()
            .await().atMost(Duration.ofSeconds(10));

        RunResponse completed = waitForCompletion(run.runId());

        long executionTime = System.currentTimeMillis() - startTime;

        // Parent should complete quickly (not wait for child)
        assertEquals("COMPLETED", completed.status());
        assertTrue(executionTime < 10000, "Parent should complete in < 10 seconds");

        // Verify child was launched
        assertEquals("DETACHED",
            completed.nodeExecutions().get("async-child").output().get("status"));
    }

    @Test
    @Order(9)
    @DisplayName("Test cancellation cascade")
    void testCancellationCascade() {
        WorkflowDefinitionResponse child1 = createSlowWorkflow(Duration.ofSeconds(60));
        WorkflowDefinitionResponse child2 = createSlowWorkflow(Duration.ofSeconds(60));

        WorkflowDefinitionResponse parentWorkflow = client.workflows()
            .create("cascade-parent")
            .version("1.0.0")
            .addNode(createSubWorkflowNode("child1", child1.definitionId()))
            .addNode(createSubWorkflowNode("child2", child2.definitionId()))
            .execute()
            .await().atMost(Duration.ofSeconds(10));

        RunResponse run = client.runs()
            .create(parentWorkflow.definitionId())
            .executeAndStart()
            .await().atMost(Duration.ofSeconds(10));

        // Wait for children to start
        Thread.sleep(2000);

        // Cancel parent
        CascadeCancellationResult result = client.runs()
            .cancelCascade(run.runId(), "Test cancellation")
            .await().atMost(Duration.ofSeconds(10));

        assertTrue(result.success());
        assertEquals(3, result.workflowsCancelled()); // Parent + 2 children
    }

    // ==================== HELPER METHODS ====================

    private WorkflowDefinitionResponse createSimpleChildWorkflow() {
        return createSimpleChildWorkflow("simple-child");
    }

    private WorkflowDefinitionResponse createSimpleChildWorkflow(String name) {
        return client.workflows()
            .create(name)
            .version("1.0.0")
            .addNode(new NodeDefinitionDto(
                "simple-task",
                "Simple Task",
                "TASK",
                "test-executor",
                Map.of(),
                List.of(),
                List.of(),
                null, 10L, false
            ))
            .addInput("input1", stringInput("Input 1", false))
            .addOutput("output1", stringOutput("Output 1"))
            .execute()
            .await().atMost(Duration.ofSeconds(10));
    }

    private WorkflowDefinitionResponse createMappingTestWorkflow() {
        return client.workflows()
            .create("mapping-test-child")
            .version("1.0.0")
            .addNode(new NodeDefinitionDto(
                "transform",
                "Transform Data",
                "TASK",
                "transformation-executor",
                Map.of(),
                List.of(),
                List.of(),
                null, 10L, false
            ))
            .addInput("childField1", stringInput("Child Field 1", true))
            .addInput("childField2", stringInput("Child Field 2", true))
            .addOutput("childResult1", stringOutput("Child Result 1"))
            .addOutput("childResult2", stringOutput("Child Result 2"))
            .execute()
            .await().atMost(Duration.ofSeconds(10));
    }

    private WorkflowDefinitionResponse createFailingWorkflow() {
        return client.workflows()
            .create("failing-workflow")
            .version("1.0.0")
            .addNode(new NodeDefinitionDto(
                "fail-task",
                "Always Fails",
                "TASK",
                "failing-executor",
                Map.of(),
                List.of(),
                List.of(),
                null, 10L, false
            ))
            .execute()
            .await().atMost(Duration.ofSeconds(10));
    }

    private WorkflowDefinitionResponse createSlowWorkflow(Duration delay) {
        return client.workflows()
            .create("slow-workflow-" + delay.getSeconds())
            .version("1.0.0")
            .addNode(new NodeDefinitionDto(
                "delay-task",
                "Slow Task",
                "TASK",
                "delay-executor",
                Map.of("delaySeconds", delay.getSeconds()),
                List.of(),
                List.of(),
                null, delay.getSeconds() + 10, false
            ))
            .execute()
            .await().atMost(Duration.ofSeconds(10));
    }

    private NodeDefinitionDto createSubWorkflowNode(
            String nodeId,
            String childWorkflowId) {
        return createSubWorkflowNode(nodeId, childWorkflowId, Map.of(), Map.of());
    }

    private NodeDefinitionDto createSubWorkflowNode(
            String nodeId,
            String childWorkflowId,
            Map<String, String> inputMapping,
            Map<String, String> outputMapping) {
        return createSubWorkflowNode(nodeId, childWorkflowId, inputMapping, outputMapping, "PROPAGATE");
    }

    private NodeDefinitionDto createSubWorkflowNode(
            String nodeId,
            String childWorkflowId,
            Map<String, String> inputMapping,
            Map<String, String> outputMapping,
            String errorStrategy) {

        return new NodeDefinitionDto(
            nodeId,
            "Sub-Workflow: " + childWorkflowId,
            "SUB_WORKFLOW",
            "sub-workflow-executor",
            Map.of(
                "subWorkflowId", childWorkflowId,
                "inputMapping", inputMapping,
                "outputMapping", outputMapping,
                "waitForCompletion", true,
                "errorStrategy", errorStrategy
            ),
            List.of(),
            List.of(),
            null, 120L, false
        );
    }

    private NodeDefinitionDto createSubWorkflowNodeWithTimeout(
            String nodeId,
            String childWorkflowId,
            long timeoutSeconds) {

        return new NodeDefinitionDto(
            nodeId,
            "Sub-Workflow with Timeout",
            "SUB_WORKFLOW",
            "sub-workflow-executor",
            Map.of(
                "subWorkflowId", childWorkflowId,
                "waitForCompletion", true,
                "timeoutSeconds", timeoutSeconds,
                "errorStrategy", "PROPAGATE"
            ),
            List.of(),
            List.of(),
            null, timeoutSeconds + 10, false
        );
    }

    private NodeDefinitionDto createFireAndForgetNode(
            String nodeId,
            String childWorkflowId) {

        return new NodeDefinitionDto(
            nodeId,
            "Fire-and-Forget Sub-Workflow",
            "SUB_WORKFLOW",
            "sub-workflow-executor",
            Map.of(
                "subWorkflowId", childWorkflowId,
                "waitForCompletion", false
            ),
            List.of(),
            List.of(),
            null, 10L, false
        );
    }

    private InputDefinitionDto stringInput(String description, boolean required) {
        return new InputDefinitionDto(
            "string",
            "string",
            required,
            null,
            description
        );
    }

    private OutputDefinitionDto stringOutput(String description) {
        return new OutputDefinitionDto(
            "string",
            "string",
            description
        );
    }

    private RunResponse waitForCompletion(String runId) {
        int maxAttempts = 60; // 60 seconds max
        int attempts = 0;

        while (attempts < maxAttempts) {
            RunResponse run = client.runs()
                .get(runId)
                .await().atMost(Duration.ofSeconds(5));

            if (isTerminal(run.status())) {
                return run;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting", e);
            }

            attempts++;
        }

        throw new RuntimeException("Workflow did not complete within timeout");
    }

    private boolean isTerminal(String status) {
        return status.equals("COMPLETED") ||
               status.equals("FAILED") ||
               status.equals("CANCELLED");
    }
}