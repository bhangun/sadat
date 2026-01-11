@Value
@Builder
public class A2AResponse {
    String messageId;
    String requestMessageId;
    Status status;
    Map<String, Object> payload;
    Duration processingTime;
    String provenanceId;
}