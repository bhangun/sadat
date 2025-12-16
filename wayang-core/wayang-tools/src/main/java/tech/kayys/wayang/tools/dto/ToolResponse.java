
record ToolResponse(
    String requestId,
    ToolStatus status,
    Map<String, Object> result,
    ErrorPayload error,
    ToolMetrics metrics
) {
    enum ToolStatus {
        SUCCESS, ERROR, TIMEOUT
    }
    
    public static ToolResponse success(String requestId, Map<String, Object> result) {
        return new ToolResponse(requestId, ToolStatus.SUCCESS, result, null, null);
    }
    
    public static ToolResponse error(ErrorPayload error) {
        return new ToolResponse(null, ToolStatus.ERROR, Map.of(), error, null);
    }
}