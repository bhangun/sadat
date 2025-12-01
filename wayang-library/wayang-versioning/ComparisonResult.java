@Value
@Builder
public class ComparisonResult {
    List<Change> changes;
    int totalChanges;
    Map<ChangeType, Long> changesByType;
}
