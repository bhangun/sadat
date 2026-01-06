package tech.kayys.wayang.nodes.services;

import java.util.List;

public class ModelApiResponse {
    private String model;
    private String content;
    private Usage usage;
    private List<Object> functionCalls;
    private List<Double> embeddings;
    private int dimensions;

    public String getModel() { return model; }
    public String getContent() { return content; }
    public Usage getUsage() { return usage; }
    public List<Object> getFunctionCalls() { return functionCalls; }
    public List<Double> getEmbeddings() { return embeddings; }
    public int getDimensions() { return dimensions; }

    public static class Usage {
        private int promptTokens;
        private int completionTokens;

        public int getPromptTokens() { return promptTokens; }
        public int getCompletionTokens() { return completionTokens; }
    }
}
