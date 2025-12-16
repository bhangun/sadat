package tech.kayys.wayang.guardrails.detector;

record Finding(
        String type,
        String value,
        int startOffset,
        int endOffset,
        double confidence) {
}