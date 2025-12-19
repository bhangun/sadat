package tech.kayys.wayang.schema.llm;

import java.util.List;

public class LLMResponse {
    private List<Choice> choices;
    private Usage usage;
    private String model;
}
