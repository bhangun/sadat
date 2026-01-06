package tech.kayys.wayang.nodes.services;

import java.util.List;

public class ModelApiRequest {
    private String model;
    private List<Object> messages;
    private int maxTokens;
    private double temperature;
    private boolean stream;
    private List<Object> functions;

    public void setModel(String model) { this.model = model; }
    public void setMessages(List<Object> messages) { this.messages = messages; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public void setStream(boolean stream) { this.stream = stream; }
    public void setFunctions(List<Object> functions) { this.functions = functions; }
}
