package tech.kayys.wayang.schema.llm;

import java.util.List;

public class LLMMessage {
    private String role;
    private String content;
    private List<ToolCall> toolCalls;
}