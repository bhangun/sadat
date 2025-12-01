@Value
@Builder
public class LintResult {
    List<LintIssue> issues;
    int totalIssues;
    Map<Severity, Long> issuesBySeverity;
    Map<LintCategory, Long> issuesByCategory;
}
