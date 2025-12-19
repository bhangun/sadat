package tech.kayys.wayang.sdk.dto;

public record SuspendRequest(
    String reason,
    String humanTaskId
) {}
