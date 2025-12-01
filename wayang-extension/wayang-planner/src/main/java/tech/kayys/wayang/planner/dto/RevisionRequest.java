record RevisionRequest(
    String feedback,
    List<String> failedNodes,
    Map<String, Object> suggestions
) {}