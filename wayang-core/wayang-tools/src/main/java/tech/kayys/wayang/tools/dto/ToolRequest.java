record ToolRequest(
    String toolName,
    String requestId,
    Map<String, Object> parameters,
    Map<String, String> secrets,
    ExecutionContext context
) {
    public ToolRequest withSecrets(Map<String, String> newSecrets) {
        return new ToolRequest(toolName, requestId, parameters, newSecrets, context);
    }
}