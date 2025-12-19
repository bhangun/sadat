package tech.kayys.agent.schema;

public record SchemaVersion(
    int major,
    int minor
) {
    public boolean isCompatibleWith(int requiredMajor) {
        return this.major == requiredMajor; // strict major-version compatibility
    }
}