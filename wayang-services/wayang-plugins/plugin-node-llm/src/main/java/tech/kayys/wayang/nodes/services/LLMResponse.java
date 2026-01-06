package tech.kayys.wayang.nodes.services;

import java.util.List;


public class LLMResponse {
    private String modelId;
    private String output;
    private int tokensIn;
    private int tokensOut;
    private double cost;
    private List<FunctionCall> functionCalls;
    
    public static class FunctionCall {
        private String name;
        private Map<String, Object> arguments;
        
        public String getName() { return name; }
        public Map<String, Object> getArguments() { return arguments; }
    }
    
    // Getters and setters
    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }
    
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    
    public int getTokensIn() { return tokensIn; }
    public void setTokensIn(int tokensIn) { this.tokensIn = tokensIn; }
    
    public int getTokensOut() { return tokensOut; }
    public void setTokensOut(int tokensOut) { this.tokensOut = tokensOut; }
    
    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }
    
    public List<FunctionCall> getFunctionCalls() { return functionCalls; }
    public void setFunctionCalls(List<FunctionCall> functionCalls) { 
        this.functionCalls = functionCalls; 
    }
}
