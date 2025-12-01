@Value
@Builder
public class A2AMessage {
    String messageId;
    String fromAgent;
    String toAgent;
    String correlationId;
    A2AMessageType type;
    A2APurpose purpose;
    Map<String, Object> payload;
    A2AContextHints contextHints;
    SecurityToken securityToken;
    Instant timestamp;
}