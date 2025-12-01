

@Value
@Builder
public class TimelineEvent {
    Instant timestamp;
    String nodeId;
    EventType type;
    String description;
    Map<String, Object> metadata;
}
