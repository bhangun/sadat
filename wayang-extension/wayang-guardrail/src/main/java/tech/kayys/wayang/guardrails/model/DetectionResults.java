
record DetectionResults(List<DetectionResult> results) {
    
    public boolean hasBlockingIssues() {
        return results.stream()
            .anyMatch(r -> r.severity() == DetectionSeverity.BLOCK);
    }
    
    public boolean hasRedactableContent() {
        return results.stream()
            .anyMatch(r -> !r.findings().isEmpty());
    }
    
    public String summary() {
        return results.stream()
            .filter(r -> r.severity() == DetectionSeverity.BLOCK)
            .map(DetectionResult::message)
            .collect(Collectors.joining("; "));
    }
    
    public List<String> detectorIds() {
        return results.stream()
            .filter(r -> r.severity() == DetectionSeverity.BLOCK)
            .map(DetectionResult::detectorId)
            .toList();
    }
}
