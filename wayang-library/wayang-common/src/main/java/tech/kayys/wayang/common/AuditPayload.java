
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditPayload(
    @NotNull Instant timestamp,
    @NotBlank String runId,
    @NotBlank String nodeId,
    @NotNull Actor actor,
    @NotBlank String event,
    @NotNull AuditLevel level,
    List<String> tags,
    Map<String, Object> metadata,
    Map<String, Object> contextSnapshot,
    String hash
) {
    public enum AuditLevel {
        INFO, WARN, ERROR, CRITICAL
    }
    
    public record Actor(
        @NotNull ActorType type,
        String id,
        String role
    ) {
        public enum ActorType {
            SYSTEM, HUMAN, AGENT
        }
    }
}