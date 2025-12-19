package tech.kayys.wayang.node.model;

/**
 * Registration options for plugin installation.
 */
@lombok.Data
@lombok.Builder
class RegistrationOptions {
    @lombok.Builder.Default
    private boolean securityScanEnabled = true;

    @lombok.Builder.Default
    private boolean skipDependencyCheck = false;

    @lombok.Builder.Default
    private boolean forceReplace = false;

    private String registeredBy;
}
