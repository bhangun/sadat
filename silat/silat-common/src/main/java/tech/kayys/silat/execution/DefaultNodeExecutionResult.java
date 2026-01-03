package tech.kayys.silat.execution;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import tech.kayys.silat.model.ErrorInfo;
import tech.kayys.silat.model.ExecutionToken;
import tech.kayys.silat.model.NodeId;
import tech.kayys.silat.model.WaitInfo;
import tech.kayys.silat.model.WorkflowRunId;

/**
 * Node Execution Result - Result from executor
 */

public record DefaultNodeExecutionResult(
                WorkflowRunId runId,
                NodeId nodeId,
                int attempt,
                NodeExecutionStatus status,
                Map<String, Object> output,
                ErrorInfo error,
                ExecutionToken executionToken) implements NodeExecutionResult {

        @Override
        public NodeExecutionStatus getStatus() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'getStatus'");
        }

        @Override
        public String getNodeId() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'getNodeId'");
        }

        @Override
        public Instant getExecutedAt() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'getExecutedAt'");
        }

        @Override
        public Duration getDuration() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'getDuration'");
        }

        @Override
        public ExecutionContext getUpdatedContext() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'getUpdatedContext'");
        }

        @Override
        public ExecutionError getError() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'getError'");
        }

        @Override
        public WaitInfo getWaitInfo() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'getWaitInfo'");
        }

        @Override
        public Map<String, Object> getMetadata() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'getMetadata'");
        }
}
