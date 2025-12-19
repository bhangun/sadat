package tech.kayys.wayang.common.spi;

import java.util.List;

public record GuardrailsConfig(
    boolean enabled,
    List<String> preCheckRules,
    List<String> postCheckRules
) {}