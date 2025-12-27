package tech.kayys.wayang.workflow.version.dto;

import java.time.Instant;

public record VersionListItem(
        String versionId,
        String version,
        String status,
        int breakingChangeCount,
        Instant createdAt) {
}