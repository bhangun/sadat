
@Value
@Builder
public class LLMResponse {
    String requestId;
    String modelId;
    String content;
    int tokensIn;
    int tokensOut;
    double cost;
    Duration latency;
    Map<String, Object> metadata;
}