package tech.kayys.wayang.workflow;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import tech.kayys.wayang.workflow.kernel.WorkflowEngine;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import tech.kayys.wayang.schema.execution.ErrorPayload;
import tech.kayys.wayang.schema.execution.ErrorPayload.ErrorType;
import tech.kayys.wayang.schema.node.EdgeDefinition;
import tech.kayys.wayang.schema.node.NodeDefinition;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.executor.NodeExecutionResult;
import tech.kayys.wayang.workflow.executor.NodeExecutor;

import tech.kayys.wayang.workflow.api.model.RunStatus;
import tech.kayys.wayang.workflow.repository.WorkflowRunRepository;
import tech.kayys.wayang.workflow.service.PolicyEngine;
import tech.kayys.wayang.workflow.service.ProvenanceService;
import tech.kayys.wayang.workflow.service.StateStore;
import tech.kayys.wayang.workflow.service.NodeContext;
import tech.kayys.wayang.workflow.service.TelemetryService;
import tech.kayys.wayang.workflow.engine.WorkflowRunManager;
import tech.kayys.wayang.workflow.service.WorkflowValidator;
import tech.kayys.wayang.workflow.service.WorkflowRegistry;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Comprehensive test suite for WorkflowEngine.
 * Tests all critical paths and edge cases.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorkflowEngineTest {

        // Center Stage
        @Inject
        WorkflowEngine workflowEngine;

        // Infrastructure Mocks
        @InjectMock
        StateStore stateStore;
        @InjectMock
        WorkflowRunRepository workflowRepository;
        @InjectMock
        io.vertx.mutiny.pgclient.PgPool pgPool;

        // Execution Mocks
        @InjectMock
        NodeExecutor nodeExecutor;
        @InjectMock
        WorkflowRunManager runManager;

        @InjectMock
        WorkflowRegistry registry;

        @InjectMock
        PolicyEngine policyEngine;

        @InjectMock
        WorkflowValidator workflowValidator;

        // Observability Mocks
        @InjectMock
        ProvenanceService provenanceService;
        @InjectMock
        TelemetryService telemetryService;

        // Test Data
        private WorkflowDefinition testWorkflow;
        private final String tenantId = "test-tenant";

        @BeforeEach
        void setup() {
                testWorkflow = createSimpleWorkflow();
                setupDefaultMocks();
                // Ensure context is clear before each test
                tech.kayys.wayang.workflow.security.context.SecurityContextHolder.clear();
        }

        @Test
        @Order(100)
        @DisplayName("Should block access on tenant mismatch")
        void testSecurityTenantMismatch() {
                // Given
                Map<String, Object> inputs = Map.of("input", "data");

                // Simulate authenticated context for "other-tenant"
                io.quarkus.security.identity.SecurityIdentity identity = Mockito
                                .mock(io.quarkus.security.identity.SecurityIdentity.class);
                java.security.Principal principal = Mockito.mock(java.security.Principal.class);
                Mockito.when(principal.getName()).thenReturn("alice");
                Mockito.when(identity.getPrincipal()).thenReturn(principal);

                tech.kayys.wayang.workflow.security.context.SecurityContextHolder.setContext(
                                tech.kayys.wayang.workflow.security.context.SecurityContextHolder.SecurityContext
                                                .fromUserAuth("other-tenant", identity));

                try {
                        // When calling with "test-tenant"
                        org.junit.jupiter.api.Assertions.assertThrows(SecurityException.class, () -> {
                                workflowEngine.start(testWorkflow, inputs, tenantId)
                                                .await().indefinitely();
                        });
                } finally {
                        tech.kayys.wayang.workflow.security.context.SecurityContextHolder.clear();
                }
        }

        @Test
        @Order(101)
        @DisplayName("Should allow access on tenant match")
        void testSecurityTenantMatch() {
                // Given
                Map<String, Object> inputs = Map.of("input", "data");

                // Simulate authenticated context for "test-tenant"
                io.quarkus.security.identity.SecurityIdentity identity = Mockito
                                .mock(io.quarkus.security.identity.SecurityIdentity.class);
                java.security.Principal principal = Mockito.mock(java.security.Principal.class);
                Mockito.when(principal.getName()).thenReturn("alice");
                Mockito.when(identity.getPrincipal()).thenReturn(principal);

                tech.kayys.wayang.workflow.security.context.SecurityContextHolder.setContext(
                                tech.kayys.wayang.workflow.security.context.SecurityContextHolder.SecurityContext
                                                .fromUserAuth(tenantId, identity));

                // Mock successful execution
                when(nodeExecutor.execute(any(NodeDefinition.class), any(NodeContext.class)))
                                .thenReturn(Uni.createFrom().item(
                                                NodeExecutionResult.success("node-1", Map.of("output", "result"))));

                try {
                        // When
                        WorkflowRun result = workflowEngine.start(testWorkflow, inputs, tenantId)
                                        .await().indefinitely();

                        // Then
                        assertNotNull(result);
                        assertEquals(RunStatus.COMPLETED, result.getStatus());
                } finally {
                        tech.kayys.wayang.workflow.security.context.SecurityContextHolder.clear();
                }
        }

        // ==========================================
        // Section 1: Basic Workflow Execution
        // ==========================================

        @Test
        @Order(1)
        @DisplayName("Should successfully execute simple linear workflow")
        void testSimpleLinearWorkflow() {
                // Given
                Map<String, Object> inputs = Map.of("input_data", "test");

                // Mock successful execution
                when(nodeExecutor.execute(any(NodeDefinition.class), any(NodeContext.class)))
                                .thenReturn(Uni.createFrom().item(
                                                NodeExecutionResult.success("node-1", Map.of("output", "result"))));

                // When
                WorkflowRun result = workflowEngine.start(testWorkflow, inputs, tenantId)
                                .subscribe().withSubscriber(UniAssertSubscriber.create())
                                .awaitItem()
                                .getItem();

                // Then
                assertNotNull(result);
                // The mock returns COMPLETED, so this assertion passes
                assertEquals(RunStatus.COMPLETED, result.getStatus());

                // Verify interactions
                verify(runManager).createRun(any(), eq(tenantId));
                verify(runManager).startRun(anyString(), eq(tenantId));
                verify(nodeExecutor, atLeastOnce()).execute(any(), any());
                verify(runManager).completeRun(anyString(), eq(tenantId), any());
        }

        @Test
        @Order(2)
        @DisplayName("Should execute multi-node workflow in correct order")
        void testMultiNodeWorkflow() {
                // Given
                WorkflowDefinition multiNodeWorkflow = createMultiNodeWorkflow();
                Map<String, Object> inputs = Map.of("start_input", "data");

                List<String> executionOrder = new ArrayList<>();

                when(nodeExecutor.execute(any(NodeDefinition.class), any(NodeContext.class)))
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

                when(nodeExecutor.execute(any(NodeDefinition.class), any(NodeContext.class)))
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

        // ==========================================
        // Section 2: Error Handling & Resilience
        // ==========================================

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
                when(nodeExecutor.execute(any(NodeDefinition.class), any(NodeContext.class)))
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

        // ==========================================
        // Section 3: Human-in-the-Loop (HITL)
        // ==========================================

        @Test
        @Order(7)
        @DisplayName("Should suspend workflow for HITL")
        void testHITLSuspension() {
                // Given
                Map<String, Object> inputs = Map.of("input", "data");

                when(nodeExecutor.execute(any(NodeDefinition.class), any(NodeContext.class)))
                                .thenReturn(Uni.createFrom()
                                                .item(NodeExecutionResult.awaitingHuman("node-1", "task-1")));

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

                when(runManager.resumeRun(anyString(), anyString(), any(), any()))
                                .thenReturn(Uni.createFrom().item(suspendedRun));

                // Mock Registry to return the test workflow
                when(registry.getWorkflowByVersion(eq("test-workflow"), eq("1.0.0")))
                                .thenReturn(Uni.createFrom().item(testWorkflow));

                // When
                WorkflowRun result = workflowEngine.resume("run-123", tenantId)
                                .subscribe().withSubscriber(UniAssertSubscriber.create())
                                .awaitItem()
                                .getItem();

                // Then
                verify(runManager, atLeast(1)).resumeRun(eq("run-123"), eq(tenantId), any(), any());
        }

        // ==========================================
        // Section 4: Policy & Guardrails
        // ==========================================

        @Test
        @Order(9)
        @DisplayName("Should block workflow on policy violation")
        void testPolicyViolation() {
                // Given
                Map<String, Object> inputs = Map.of("input", "data");

                tech.kayys.wayang.workflow.model.GuardrailResult blockedResult = Mockito
                                .mock(tech.kayys.wayang.workflow.model.GuardrailResult.class);
                when(blockedResult.isAllowed()).thenReturn(false);
                when(blockedResult.getReason()).thenReturn("Rate limit exceeded");

                when(policyEngine.validateWorkflowStart(any(), any()))
                                .thenReturn(Uni.createFrom().item(blockedResult));

                // When
                // When & Then
                org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
                        workflowEngine.start(testWorkflow, inputs, tenantId)
                                        .await().indefinitely();
                });

                verify(nodeExecutor, never()).execute(any(), any());
                verify(runManager, never()).createRun(any(), any());
        }

        // ==========================================
        // Section 5: Lifecycle Management
        // ==========================================

        @Test
        @Order(11)
        @DisplayName("Should pause workflow execution")
        void testPauseWorkflow() {
                // Given
                String runId = "run-123";

                when(runManager.updateRunStatus(eq(runId), anyString(), eq(RunStatus.PAUSED)))
                                .thenReturn(Uni.createFrom().voidItem());

                // When
                workflowEngine.pause(runId, tenantId)
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
                workflowEngine.cancel(runId, tenantId, "cancelled by test")
                                .subscribe().withSubscriber(UniAssertSubscriber.create())
                                .awaitItem();

                // Then
                verify(runManager).cancelRun(eq(runId), anyString(), anyString());
        }

        // ==========================================
        // Section 6: Edge Cases
        // ==========================================

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

        // ==========================================
        // Helpers & Factories
        // ==========================================

        private void setupDefaultMocks() {
                // State store mocks - keep for legacy or indirect dependencies if any
                when(stateStore.save(any(WorkflowRun.class)))
                                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0)));

                when(stateStore.saveCheckpoint(anyString(), any()))
                                .thenReturn(Uni.createFrom().voidItem());

                // Provenance mocks
                when(provenanceService.logWorkflowStart(any(), any())).thenReturn(Uni.createFrom().voidItem());
                when(provenanceService.logWorkflowComplete(any())).thenReturn(Uni.createFrom().voidItem());
                when(provenanceService.logRunFailed(any(), any())).thenReturn(Uni.createFrom().voidItem());
                when(provenanceService.logNodeSuccess(any(), any())).thenReturn(Uni.createFrom().voidItem());
                when(provenanceService.logNodeError(any(), any(), any())).thenReturn(Uni.createFrom().voidItem());
                when(provenanceService.logNodeBlocked(any(), any())).thenReturn(Uni.createFrom().voidItem());
                when(provenanceService.logRunResumed(any())).thenReturn(Uni.createFrom().voidItem());
                when(provenanceService.logStatusChange(any(), any(), any())).thenReturn(Uni.createFrom().voidItem());

                // Policy engine mocks
                tech.kayys.wayang.workflow.model.GuardrailResult mockPolicyResult = Mockito
                                .mock(tech.kayys.wayang.workflow.model.GuardrailResult.class);
                when(mockPolicyResult.isAllowed()).thenReturn(true);
                when(policyEngine.validateWorkflowStart(any(), any()))
                                .thenReturn(Uni.createFrom().item(mockPolicyResult));

                // Telemetry mocks (void methods)
                doNothing().when(telemetryService).recordWorkflowStart(any());
                doNothing().when(telemetryService).recordWorkflowCompletion(any(), anyLong());
                doNothing().when(telemetryService).recordNodeExecution(any(), any(), anyLong(), any());

                // Workflow validator mock
                tech.kayys.wayang.sdk.util.WorkflowValidator.ValidationResult mockValidationResult = Mockito
                                .mock(tech.kayys.wayang.sdk.util.WorkflowValidator.ValidationResult.class);
                when(mockValidationResult.isValid()).thenReturn(true);
                when(workflowValidator.validate(any())).thenReturn(Uni.createFrom().item(mockValidationResult));

                // Run manager mock - Essential for WorkflowEngine
                when(runManager.createRun(any(), anyString())).thenAnswer(invocation -> {
                        tech.kayys.wayang.workflow.api.dto.CreateRunRequest req = invocation.getArgument(0);
                        String tId = invocation.getArgument(1);
                        return Uni.createFrom().item(WorkflowRun.builder()
                                        .runId("test-run-" + java.util.UUID.randomUUID().toString().substring(0, 8))
                                        .workflowId(req.getWorkflowId())
                                        .workflowVersion(req.getWorkflowVersion())
                                        .tenantId(tId)
                                        .status(RunStatus.PENDING)
                                        .inputs(req.getInputs())
                                        .build());
                });

                when(runManager.startRun(anyString(), anyString())).thenAnswer(invocation -> {
                        String rId = invocation.getArgument(0);
                        String tId = invocation.getArgument(1);
                        return Uni.createFrom().item(WorkflowRun.builder()
                                        .runId(rId)
                                        .tenantId(tId)
                                        .status(RunStatus.RUNNING)
                                        .build());
                });

                when(runManager.completeRun(anyString(), anyString(), any())).thenAnswer(invocation -> {
                        String rId = invocation.getArgument(0);
                        return Uni.createFrom().item(WorkflowRun.builder()
                                        .runId(rId)
                                        .status(RunStatus.COMPLETED)
                                        .completedAt(Instant.now())
                                        .build());
                });

                when(runManager.failRun(anyString(), anyString(), any())).thenAnswer(invocation -> {
                        String rId = invocation.getArgument(0);
                        tech.kayys.wayang.schema.execution.ErrorPayload error = invocation.getArgument(2);
                        return Uni.createFrom().item(WorkflowRun.builder()
                                        .runId(rId)
                                        .status(RunStatus.FAILED)
                                        .errorMessage(error != null ? error.getMessage() : null)
                                        .completedAt(Instant.now())
                                        .build());
                });

                when(runManager.updateRunStatus(anyString(), anyString(), any()))
                                .thenReturn(Uni.createFrom().voidItem());

                when(runManager.cancelRun(anyString(), anyString(), anyString()))
                                .thenReturn(Uni.createFrom().voidItem());

                when(runManager.suspendRun(anyString(), anyString(), anyString(), any()))
                                .thenReturn(Uni.createFrom()
                                                .item(WorkflowRun.builder().status(RunStatus.SUSPENDED).build()));
        }

        private WorkflowDefinition createSimpleWorkflow() {
                NodeDefinition node = new NodeDefinition();
                node.setId("node-1");
                node.setType("test-node");
                node.setDisplayName("Test Node");

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
