package tech.kayys.wayang.plugin.node;

import java.util.Map;

public class CheckpointState {
        private NodeStatus status;
        private int attempt;
        private String checkpointRef;
        private Map<String, Object> inputSnapshot;
        private Map<String, Object> outputSnapshot;

        public CheckpointState(
            NodeStatus status,
            int attempt,
            String checkpointRef,
            Map<String, Object> inputSnapshot,
            Map<String, Object> outputSnapshot
        ) {
            this.status = status;
            this.attempt = attempt;
            this.checkpointRef = checkpointRef;
            this.inputSnapshot = inputSnapshot;
            this.outputSnapshot = outputSnapshot;
        }
        public NodeStatus getStatus() {
            return this.status;
        }

        public void setStatus(NodeStatus status) {
            this.status = status;
        }

        public int getAttempt() {
            return this.attempt;
        }

        public void setAttempt(int attempt) {
            this.attempt = attempt;
        }

        public String getCheckpointRef() {
            return this.checkpointRef;
        }

        public void setCheckpointRef(String checkpointRef) {
            this.checkpointRef = checkpointRef;
        }

        public Map<String, Object> getInputSnapshot() {
            return this.inputSnapshot;
        }

        public void setInputSnapshot(Map<String, Object> inputSnapshot) {
            this.inputSnapshot = inputSnapshot;
        }

        public Map<String, Object> getOutputSnapshot() {
            return this.outputSnapshot;
        }

        public void setOutputSnapshot(Map<String, Object> outputSnapshot) {
            this.outputSnapshot = outputSnapshot;
        }
    }