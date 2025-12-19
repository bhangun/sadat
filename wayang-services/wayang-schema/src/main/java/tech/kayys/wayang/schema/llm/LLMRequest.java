package tech.kayys.wayang.schema.llm;

import java.util.List;

public class LLMRequest {
    private String model;
    private List<LLMMessage> messages;
    private Double temperature;
    private Integer maxTokens;
    private List<String> stopSequences;
    private List<ToolDefinition> tools;
}