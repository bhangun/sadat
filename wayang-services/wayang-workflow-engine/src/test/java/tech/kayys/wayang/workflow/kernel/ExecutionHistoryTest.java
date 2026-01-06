package tech.kayys.wayang.workflow.kernel;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionHistoryTest {

    @Test
    void testHasErrors_WhenNoErrors() {
        ExecutionHistory history = ExecutionHistory.empty(
                WorkflowRunId.of("run-1"),
                WorkflowId.of("wf-1"),
                "tenant-1");

        assertFalse(history.hasErrors());
    }

    @Test
    void testHasErrors_WithNodeExecutionError() {
        ExecutionHistory history = ExecutionHistory.empty(
                WorkflowRunId.of("run-1"),
                WorkflowId.of("wf-1"),
                "tenant-1");

        NodeExecutionRecord record = NodeExecutionRecord.builder()
                .nodeId("node-1")
                .status(NodeExecutionStatus.FAILED)
                .build();

        history = history.addNodeExecution(record);

        assertTrue(history.hasErrors());
    }

    @Test
    void testHasErrors_WithEventError() {
        ExecutionHistory history = ExecutionHistory.empty(
                WorkflowRunId.of("run-1"),
                WorkflowId.of("wf-1"),
                "tenant-1");

        ExecutionHistory.ExecutionEvent event = ExecutionHistory.ExecutionEvent.builder()
                .eventId("evt-1")
                .eventType(ExecutionHistory.ExecutionEvent.ExecutionEventType.ERROR_OCCURRED)
                .timestamp(Instant.now())
                .build();

        history = history.addEvent(event);

        assertTrue(history.hasErrors());
    }

    @Test
    void testHasErrors_WithRunFailedEvent() {
        ExecutionHistory history = ExecutionHistory.empty(
                WorkflowRunId.of("run-1"),
                WorkflowId.of("wf-1"),
                "tenant-1");

        ExecutionHistory.ExecutionEvent event = ExecutionHistory.ExecutionEvent.builder()
                .eventId("evt-1")
                .eventType(ExecutionHistory.ExecutionEvent.ExecutionEventType.RUN_FAILED)
                .timestamp(Instant.now())
                .build();

        history = history.addEvent(event);

        assertTrue(history.hasErrors());
    }

    @Test
    void testToSummary() {
        WorkflowRunId runId = WorkflowRunId.of("run-1");
        WorkflowId wfId = WorkflowId.of("wf-1");
        ExecutionHistory history = ExecutionHistory.empty(runId, wfId, "tenant-1");

        Map<String, Object> summary = history.toSummary();

        assertEquals("run-1", summary.get("runId"));
        assertEquals("wf-1", summary.get("workflowId"));
        assertEquals("tenant-1", summary.get("tenantId"));
        assertNotNull(summary.get("status"));
    }
}
