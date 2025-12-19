package tech.kayys.wayang.schema.governance;

import java.util.Map;

public class SimulationConfig {
    private Boolean enabled = false;
    private Map<String, Object> mockResponses;
    private String seed;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, Object> getMockResponses() {
        return mockResponses;
    }

    public void setMockResponses(Map<String, Object> mockResponses) {
        this.mockResponses = mockResponses;
    }

    public String getSeed() {
        return seed;
    }

    public void setSeed(String seed) {
        this.seed = seed;
    }
}
