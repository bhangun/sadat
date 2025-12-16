package tech.kayys.wayang.plugin.guardrails;


/**
 * Configuration for Guardrails plugin
 */
public record GuardrailsConfig(
    boolean enabled,
    boolean scanPII,
    boolean moderateContent,
    boolean enforceCapabilities
) {
    public static final GuardrailsConfig DEFAULT = new GuardrailsConfig(true, false, false, true);

    public static GuardrailsConfig disabled() {
        return new GuardrailsConfig(false, false, false, false);
    }
}