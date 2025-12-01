
@Value
@Builder
public class LintIssue {
    Severity severity;
    LintCategory category;
    String message;
    String location;
    Optional<String> recommendation;
    Map<String, Object> metadata;
}

