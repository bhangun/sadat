package tech.kayys.wayang.workflow.version.dto;

public record PublishRequest(
                boolean canaryDeployment,
                int canaryPercentage,
                boolean autoMigrate,
                String publishedBy) {
}