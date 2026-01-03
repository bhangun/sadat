package tech.kayys.silat.model;

public record ErrorSnapshot(
        String code,
        String message,
        String stackTrace) {
}
