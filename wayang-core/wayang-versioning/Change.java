@Value
@Builder
public class Change {
    ChangeType type;
    String nodeId;
    String description;
    Object before;
    Object after;
}
