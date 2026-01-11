package tech.kayys.silat.executor.camel.ai;

record Entity(
    String text,
    String type,
    double confidence
) {}