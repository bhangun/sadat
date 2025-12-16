package tech.kayys.wayang.plugin.node;

import java.util.Map;

/**
 * Node Example
 */
public class NodeExample {
    
    private String title;
    private String description;
    private Map<String, Object> configuration;
    private Map<String, Object> sampleInput;
    private Map<String, Object> expectedOutput;
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public Map<String, Object> getSampleInput() {
        return sampleInput;
    }

    public void setSampleInput(Map<String, Object> sampleInput) {
        this.sampleInput = sampleInput;
    }

    public Map<String, Object> getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(Map<String, Object> expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

}