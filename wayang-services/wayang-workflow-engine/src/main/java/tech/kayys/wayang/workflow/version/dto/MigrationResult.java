package tech.kayys.wayang.workflow.version.dto;

import java.util.List;

public record MigrationResult(
        int totalRuns,
        int successfulMigrations,
        int failedMigrations,
        List<String> errors) {
}