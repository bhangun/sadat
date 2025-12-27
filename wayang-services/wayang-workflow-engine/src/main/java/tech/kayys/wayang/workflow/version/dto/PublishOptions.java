package tech.kayys.wayang.workflow.version.dto;

public record PublishOptions(
        boolean canaryDeployment,
        int canaryPercentage,
        boolean autoMigrate,
        String publishedBy) {
    public static PublishOptions defaults() {
        return new PublishOptions(false, 0, false, null);
    }
}
