package tech.kayys.wayang.plugin;

/**
 * Recovery Result
 */
public class RecoveryResult {
    

    private boolean recovered = false;
    
    private String message;
    private Object recoveredState;
    
    public static RecoveryResult success(Object state) {
        return RecoveryResult.builder()
            .recovered(true)
            .recoveredState(state)
            .build();
    }
    
    public static RecoveryResult failed(String message) {
        return RecoveryResult.builder()
            .recovered(false)
            .message(message)
            .build();
    }

    public boolean isRecovered() {
        return recovered;
    }

    public void setRecovered(boolean recovered) {
        this.recovered = recovered;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getRecoveredState() {
        return recoveredState;
    }

    public void setRecoveredState(Object recoveredState) {
        this.recoveredState = recoveredState;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean recovered;
        private String message;
        private Object recoveredState;

        public Builder recovered(boolean recovered) {
            this.recovered = recovered;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder recoveredState(Object recoveredState) {
            this.recoveredState = recoveredState;
            return this;
        }

        public RecoveryResult build() {
            RecoveryResult result = new RecoveryResult();
            result.setRecovered(recovered);
            result.setMessage(message);
            result.setRecoveredState(recoveredState);
            return result;
        }
    }
}