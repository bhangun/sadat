package tech.kayys.wayang.workflow;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import tech.kayys.wayang.workflow.api.model.RunStatus;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import tech.kayys.wayang.schema.node.EdgeDefinition;
import tech.kayys.wayang.schema.execution.ErrorPayload;
import tech.kayys.wayang.schema.execution.ErrorPayload.ErrorType;
import tech.kayys.wayang.schema.node.NodeDefinition;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import tech.kayys.wayang.workflow.service.WorkflowEngine;
import tech.kayys.wayang.workflow.service.NodeExecutionResult;
import tech.kayys.wayang.workflow.service.NodeExecutor;
import tech.kayys.wayang.workflow.service.ProvenanceService;
import tech.kayys.wayang.workflow.service.ProvenanceService.*;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.repository.WorkflowRunRepository;
import tech.kayys.wayang.workflow.service.*;
import tech.kayys.wayang.workflow.model.GuardrailResult;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.mockito.ArgumentMatchers;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for WorkflowEngine.
 * Tests all critical paths and edge cases.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorkflowEngineTest {

        @Inject
        WorkflowEngine workflowEngine;

        @InjectMock
        StateStore stateStore;

        @InjectMock
        NodeExecutor nodeExecutor;

        @InjectMock
        ProvenanceService provenanceService;

        @InjectMock
        TelemetryService telemetryService;

        @InjectMock
        PolicyEngine policyEngine;

        @InjectMock
        WorkflowValidator workflowValidator;

        @InjectMock
        WorkflowRunRepository workflowRepository;

        @InjectMock
        WorkflowRunManager runManager;

        private WorkflowDefinition testWorkflow;
        private String tenantId = "test-tenant";

        @BeforeEach
        void setup() {
                testWorkflow = createSimpleWorkflow();
                setupDefaultMocks();
        }

        // ========== Basic Workflow Execution Tests ==========

        @Test
        @Order(1)
        @DisplayName("Should successfully execute simple linear workflow")
        void testSimpleLinearWorkflow() {
                // Given
                Map<String, Object> inputs = Map.of("input_data", "test");

                // Mock successful execution
                when(nodeExecutor.execute(any(NodeDefinition.class),
                                any(tech.kayys.wayang.workflow.service.NodeContext.class)))
                                .thenReturn(Uni.createFrom().item(
                                                NodeExecutionResult.success("node-1", Map.of("output", "result"))));

                // When
                WorkflowRun result = workflowEngine.start(testWorkflow, inputs, tenantId)
                                .subscribe().withSubscriber(UniAssertSubscriber.create())
                                .awaitItem()
                                .getItem();

                // Then
                assertNotNull(result);
                assertEquals(RunStatus.COMPLETED, result.getStatus());
                assertNotNull(result.getCompletedAt());

                // Verify interactions
                verify(stateStore, atLeastOnce()).save(any(WorkflowRun.class));
                verify(nodeExecutor, atLeastOnce()).execute(any(), any());
                verify(provenanceService).logWorkflowStart(any(), any());
                verify(provenanceService).logWorkflowComplete(any());
        }

        @Test
        @Order(2)
        @DisplayName("Should execute multi-node workflow in correct order")
        void testMultiNodeWorkflow() {
                // Given
                WorkflowDefinition multiNodeWorkflow = createMultiNodeWorkflow();
                Map<String, Object> inputs = Map.of("start_input", "data");

                List<String> executionOrder = new ArrayList<>();

                when(nodeExecutor.execute(any(NodeDefinition.class),
                                any(tech.kayys.wayang.workflow.service.NodeContext.class)))
                                .thenAnswer(invocation -> {
                                        NodeDefinition nodeDef = invocation.getArgument(0);
                                        executionOrder.add(nodeDef.getId());
                                        return Uni.createFrom().item(
                                                        NodeExecutionResult.success(
                                                                        nodeDef.getId(),
                                                                        Map.of("output", "result-" + nodeDef.getId())));
                                });

                // When
                WorkflowRun result = workflowEngine.start(multiNodeWorkflow, inputs, tenantId)
                                .subscribe().withSubscriber(UniAssertSubscriber.create())
                                .awaitItem()
                                .getItem();

                // Then
                assertEquals(RunStatus.COMPLETED, result.getStatus());
                assertEquals(3, executionOrder.size());

                // Verify topological order
                int node1Index = executionOrder.indexOf("node-1");
                int node2Index = executionOrder.indexOf("node-2");
                int node3Index = executionOrder.indexOf("node-3");

                assertTrue(node1Index < node2Index, "node-1 should execute before node-2");
                assertTrue(node2Index < node3Index, "node-2 should execute before node-3");
        }

        @Test
        @Order(3)
        @DisplayName("Should handle parallel node execution")
        void testParallelExecution() {
                // Given
                WorkflowDefinition parallelWorkflow = createParallelWorkflow();
                Map<String, Object> inputs = Map.of("input", "data");

                Set<String> executedNodes = Collections.synchronizedSet(new HashSet<>());

                when(nodeExecutor.execute(any(NodeDefinition.class),
                                any(tech.kayys.wayang.workflow.service.NodeContext.class)))
                                .thenAnswer(invocation -> {
                                        NodeDefinition nodeDef = invocation.getArgument(0);
                                        executedNodes.add(nodeDef.getId());

                                        // Simulate some work
                                        return Uni.createFrom().item(
                                                        NodeExecutionResult.success(
                                                                        nodeDef.getId(),
                                                                        Map.of("output", "result")))
                                                        .onItem().delayIt().by(java.time.Duration.ofMillis(10));
                                });

                // When
                WorkflowRun result = workflowEngine.start(parallelWorkflow, inputs, tenantId)
                                .subscribe().withSubscriber(UniAssertSubscriber.create())
                                .awaitItem()
                                .getItem();

                // Then
                assertEquals(RunStatus.COMPLETED, result.getStatus());
                assertEquals(4, executedNodes.size()); // entry + 2 parallel + join
        }

        // ========== Error Handling Tests ==========

        @Test
        @Order(4)
        @DisplayName("Should handle node error with retry")
        void testNodeErrorWithRetry() {
                // Given
                Map<String, Object> inputs = Map.of("input", "data");

                ErrorPayload error = ErrorPayload.builder()
                                .type(ErrorType.VALIDATION_ERROR)
                                .message("Validation failed")
                                .originNode("node-1")
                                .retryable(true)
                                .attempt(0)
                                .maxAttempts(3)
                                .timestamp(LocalDateTime.now())
                                .build();

                // First call fails, second succeeds
                when(nodeExecutor.execute(any(NodeDefinition.class),
                                any(tech.kayys.wayang.workflow.service.NodeContext.class)))
                                .thenReturn(
                                                Uni.createFrom().item(NodeExecutionResult.error("node-1", error)),
                                                Uni.createFrom().item(NodeExecutionResult.success("node-1",
                                                                Map.of("output", "success"))));

                // When
                WorkflowRun result = workflowEngine.start(testWorkflow, inputs, tenantId)
                                .subscribe().withSubscriber(UniAssertSubscriber.create())
                                .awaitItem()
                                .getItem();

                // Then
                assertEquals(RunStatus.COMPLETED, result.getStatus());
                verify(nodeExecutor, Mockito.atLeastOnce()).execute(any(), any());
        }

        // ========== HITL Tests ==========

        @Test
        @Order(7)
        @DisplayName("Should suspend workflow for HITL")
        void testHITLSuspension() {
                // Given
                Map<String, Object> inputs = Map.of("input", "data");

                ErrorPayload error = ErrorPayload.builder()
                                .type(ErrorType.VALIDATION_ERROR)
                                .message("Requires human review")
                                .originNode("node-1")
                                .retryable(false)
                                .timestamp(LocalDateTime.now())
                                .build();

                when(nodeExecutor.execute(any(NodeDefinition.class),
                                any(tech.kayys.wayang.workflow.service.NodeContext.class)))
                                .thenReturn(Uni.createFrom().item(NodeExecutionResult.error("node-1", error)));

                // When
                WorkflowRun result = workflowEngine.start(testWorkflow, inputs, tenantId)
                                .subscribe().withSubscriber(UniAssertSubscriber.create())
                                .awaitItem()
                                .getItem();

                // Then
                assertEquals(RunStatus.SUSPENDED, result.getStatus());
        }

        @Test
        @Order(8)
        @DisplayName("Should resume workflow after HITL completion")
        void testHITLResumption() {
                // Given - Create a suspended workflow
                WorkflowRun suspendedRun = WorkflowRun.builder()
                                .runId("run-123")
                                .workflowId("test-workflow")
                                .workflowVersion("1.0.0")
                                .tenantId(tenantId)
                                .status(RunStatus.SUSPENDED)
                                .createdAt(Instant.now())
                                .build();

                when(stateStore.load("run-123"))
                                .thenReturn(Uni.createFrom().item(suspendedRun));

                when(workflowRepository.findById(anyString(), anyString()))
                                .thenReturn(Uni.createFrom().item(suspendedRun)); // Mock loading by ID

                when(nodeExecutor.execute(any(NodeDefinition.class),
                                any(tech.kayys.wayang.workflow.service.NodeContext.class)))
                                .thenReturn(Uni.createFrom().item(
                                                NodeExecutionResult.success("node-1", Map.of("output", "result"))));

                // When
                WorkflowRun result = workflowEngine.resume("run-123")
                                .subscribe().withSubscriber(UniAssertSubscriber.create())
                                .awaitItem()
                                .getItem();

                // Then
                verify(stateStore, atLeast(1)).save(any(WorkflowRun.class));
                verify(provenanceService).logRunResumed(any());
        }

        // ========== Policy and Validation Tests ==========

        @Test
        @Order(9)
        @DisplayName("Should block workflow on policy violation")
        void testPolicyViolation() {
                // Given
                Map<String, Object> inputs = Map.of("input", "data");

                when(policyEngine.validateWorkflowStart(any(), any()))
                                .thenReturn(Uni.createFrom().item(
                                                GuardrailResult.block("Rate limit exceeded")));

                // When
                WorkflowRun result = workflowEngine.start(testWorkflow, inputs, tenantId)
                                .subscribe().withSubscriber(UniAssertSubscriber.create())
                                .awaitItem()
                                .getItem();

                // Then
                assertEquals(RunStatus.BLOCKED, result.getStatus());
                assertTrue(result.getErrorMessage().contains("Policy violation"));
                verify(nodeExecutor, never()).execute(any(), any());
        }

        // ========== Pause/Resume/Cancel Tests ==========

        @Test
        @Order(11)
        @DisplayName("Should pause workflow execution")
        void testPauseWorkflow() {
                // Given
                String runId = "run-123";

                when(runManager.updateRunStatus(eq(runId), anyString(), eq(RunStatus.PAUSED)))
                                .thenReturn(Uni.createFrom().voidItem());

                // When
                workflowEngine.pause(runId)
                                .subscribe().withSubscriber(UniAssertSubscriber.create())
                                .awaitItem();

                // Then
                verify(runManager).updateRunStatus(eq(runId), anyString(), eq(RunStatus.PAUSED));
        }

        @Test
        @Order(12)
        @DisplayName("Should cancel workflow execution")
        void testCancelWorkflow() {
                // Given
                String runId = "run-123";

                when(runManager.cancelRun(eq(runId), anyString(), anyString()))
                                .thenReturn(Uni.createFrom().voidItem());

                // When
                workflowEngine.cancel(runId)
                                .subscribe().withSubscriber(UniAssertSubscriber.create())
                                .awaitItem();

                // Then
                verify(runManager).cancelRun(eq(runId), anyString(), anyString());
        }

        // ========== Edge Cases and Error Conditions ==========

        @Test
        @Order(13)
        @DisplayName("Should handle workflow with no entry nodes")
        void testNoEntryNodes() {
                // Given
                WorkflowDefinition noEntryWorkflow = createCircularWorkflow();
                Map<String, Object> inputs = Map.of("input", "data");

                // When
                WorkflowRun result = workflowEngine.start(noEntryWorkflow, inputs, tenantId)
                                .subscribe().withSubscriber(UniAssertSubscriber.create())
                                .awaitItem()
                                .getItem();

                // Then
                assertEquals(RunStatus.FAILED, result.getStatus());
                assertTrue(result.getErrorMessage().contains("No entry nodes"));
        }

        // ========== Helper Methods ==========

        private void setupDefaultMocks() {
                // State store mocks
                when(stateStore.save(any(WorkflowRun.class)))
                                .thenAnswer(invocation -> {
                                        WorkflowRun run = invocation.getArgument(0);
                                        return Uni.createFrom().item(run);
                                });

                when(stateStore.saveCheckpoint(anyString(), any()))
                                .thenReturn(Uni.createFrom().voidItem());

                // Provenance mocks
                when(provenanceService.logWorkflowStart(any(), any()))
                                .thenReturn(Uni.createFrom().voidItem());
                when(provenanceService.logWorkflowComplete(any()))
                                .thenReturn(Uni.createFrom().voidItem());
                when(provenanceService.logRunFailed(any(), any()))
                                .thenReturn(Uni.createFrom().voidItem());
                when(provenanceService.logNodeSuccess(any(), any()))
                                .thenReturn(Uni.createFrom().voidItem());
                when(provenanceService.logNodeError(any(), any(), any()))
                                .thenReturn(Uni.createFrom().voidItem());
                when(provenanceService.logNodeBlocked(any(), any()))
                                .thenReturn(Uni.createFrom().voidItem());
                when(provenanceService.logRunResumed(any()))
                                .thenReturn(Uni.createFrom().voidItem());
                when(provenanceService.logStatusChange(any(), any(), any()))
                                .thenReturn(Uni.createFrom().voidItem());

                // Policy engine mocks
                when(policyEngine.validateWorkflowStart(any(), any()))
                                .thenReturn(Uni.createFrom().item(GuardrailResult.allow()));

                // Workflow validator mock
                when(workflowValidator.validate(any()))
                                .thenReturn(Uni.createFrom()
                                                .item(tech.kayys.wayang.sdk.util.WorkflowValidator.ValidationResult
                                                                .success()));

                // Telemetry mocks (void methods)
                doNothing().when(telemetryService).recordWorkflowStart(any());
                doNothing().when(telemetryService).recordWorkflowCompletion(any(), anyLong());
                doNothing().when(telemetryService).recordNodeExecution(any(), any(), anyLong(), any());

                // Run manager mock
                when(runManager.updateRunStatus(anyString(), anyString(), any()))
                                .thenReturn(Uni.createFrom().voidItem());

                when(runManager.cancelRun(anyString(), anyString(), anyString()))
                                .thenReturn(Uni.createFrom().voidItem());
        }

        private WorkflowDefinition createSimpleWorkflow() {
                NodeDefinition node = new NodeDefinition();
                node.setId("node-1");
                node.setType("test-node");
                node.setInputs(new HashMap<>());
                node.setOutputs(new HashMap<>());

                WorkflowDefinition workflow = new WorkflowDefinition();
                workflow.setId("test-workflow");
                workflow.setName("Test Workflow");
                workflow.setVersion("1.0.0");
                workflow.setNodes(List.of(node));
                workflow.setEdges(List.of());

                return workflow;
        }

        private WorkflowDefinition createMultiNodeWorkflow() {
                // Create linear workflow: node-1 -> node-2 -> node-3
                NodeDefinition node1 = new NodeDefinition();
                node1.setId("node-1");
                node1.setType("test-node");

                NodeDefinition node2 = new NodeDefinition();
                node2.setId("node-2");
                node2.setType("test-node");

                NodeDefinition node3 = new NodeDefinition();
                node3.setId("node-3");
                node3.setType("test-node");

                EdgeDefinition edge1 = new EdgeDefinition();
                edge1.setFrom("node-1");
                edge1.setTo("node-2");
                edge1.setFromPort("success");
                edge1.setToPort("input");

                EdgeDefinition edge2 = new EdgeDefinition();
                edge2.setFrom("node-2");
                edge2.setTo("node-3");
                edge2.setFromPort("success");
                edge2.setToPort("input");

                WorkflowDefinition workflow = new WorkflowDefinition();
                workflow.setId("multi-node-workflow");
                workflow.setName("Multi Node Workflow");
                workflow.setVersion("1.0.0");
                workflow.setNodes(List.of(node1, node2, node3));
                workflow.setEdges(List.of(edge1, edge2));

                return workflow;
        }

        private WorkflowDefinition createParallelWorkflow() {
                // entry -> [parallel-1, parallel-2] -> join
                NodeDefinition entry = new NodeDefinition();
                entry.setId("entry");
                entry.setType("test");
                NodeDefinition p1 = new NodeDefinition();
                p1.setId("parallel-1");
                p1.setType("test");
                NodeDefinition p2 = new NodeDefinition();
                p2.setId("parallel-2");
                p2.setType("test");
                NodeDefinition join = new NodeDefinition();
                join.setId("join");
                join.setType("test");

                EdgeDefinition e1 = new EdgeDefinition();
                e1.setFrom("entry");
                e1.setTo("parallel-1");
                e1.setFromPort("success");
                e1.setToPort("input");
                EdgeDefinition e2 = new EdgeDefinition();
                e2.setFrom("entry");
                e2.setTo("parallel-2");
                e2.setFromPort("success");
                e2.setToPort("input");
                EdgeDefinition e3 = new EdgeDefinition();
                e3.setFrom("parallel-1");
                e3.setTo("join");
                e3.setFromPort("success");
                e3.setToPort("input1");
                EdgeDefinition e4 = new EdgeDefinition();
                e4.setFrom("parallel-2");
                e4.setTo("join");
                e4.setFromPort("success");
                e4.setToPort("input2");

                WorkflowDefinition workflow = new WorkflowDefinition();
                workflow.setId("parallel-workflow");
                workflow.setNodes(List.of(entry, p1, p2, join));
                workflow.setEdges(List.of(e1, e2, e3, e4));
                return workflow;
        }

        private WorkflowDefinition createCircularWorkflow() {
                // All nodes have incoming edges - no entry point
                NodeDefinition n1 = new NodeDefinition();
                n1.setId("node-1");
                n1.setType("test");
                NodeDefinition n2 = new NodeDefinition();
                n2.setId("node-2");
                n2.setType("test");

                EdgeDefinition e1 = new EdgeDefinition();
                e1.setFrom("node-1");
                e1.setTo("node-2");
                e1.setFromPort("success");
                e1.setToPort("input");
                EdgeDefinition e2 = new EdgeDefinition();
                e2.setFrom("node-2");
                e2.setTo("node-1");
                e2.setFromPort("success");
                e2.setToPort("input");

                WorkflowDefinition workflow = new WorkflowDefinition();
                workflow.setId("circular-workflow");
                workflow.setNodes(List.of(n1, n2));
                workflow.setEdges(List.of(e1, e2));
                return workflow;
        }
}
