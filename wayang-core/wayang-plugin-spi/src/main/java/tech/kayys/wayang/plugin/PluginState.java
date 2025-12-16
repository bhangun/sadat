package tech.kayys.wayang.plugin;

import java.util.Map;

/**
 * Plugin State (for hot reload)
 */
public class PluginState {

    private Map<String, Object> state;
    private byte[] serializedState;

    public static PluginState empty() {
        return PluginState.builder()
                .state(Map.of())
                .build();
    }

    public Map<String, Object> getState() {
        return state;
    }

    public void setState(Map<String, Object> state) {
        this.state = state;
    }

    public byte[] getSerializedState() {
        return serializedState;
    }

    public void setSerializedState(byte[] serializedState) {
        this.serializedState = serializedState;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<String, Object> state;
        private byte[] serializedState;

        public Builder state(Map<String, Object> state) {
            this.state = state;
            return this;
        }

        public Builder serializedState(byte[] serializedState) {
            this.serializedState = serializedState;
            return this;
        }

        public PluginState build() {
            PluginState pluginState = new PluginState();
            pluginState.setState(state);
            pluginState.setSerializedState(serializedState);
            return pluginState;
        }
    }
}
