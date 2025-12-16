public interface WorkflowLinter {
    LintResult lint(Workflow workflow);
    List<Suggestion> suggest(Workflow workflow);
    OptimizationResult optimize(Workflow workflow);
}