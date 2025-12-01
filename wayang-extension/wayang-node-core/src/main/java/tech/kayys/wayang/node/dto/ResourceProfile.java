/**
 * Resource requirements
 */
record ResourceProfile(
    String cpu,
    String memory,
    String gpu,
    Integer timeout,
    Map<String, String> limits
) {
    public ResourceProfile {
        limits = limits != null ? Map.copyOf(limits) : Map.of();
    }
}