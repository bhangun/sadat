package tech.kayys.wayang.workflow.version.dto;

import java.time.Instant;
import java.util.List;

public record VersionResponse(
                String versionId,
                String workflowId,
                String version,
                String previousVersion,
                String status,
                List<BreakingChange> breakingChanges,
                List<String> deprecationWarnings,
                int canaryPercentage,
                Instant createdAt,
                Instant publishedAt) {
}