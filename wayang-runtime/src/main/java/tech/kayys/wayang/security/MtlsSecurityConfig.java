package tech.kayys.wayang.security;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.List;

/**
 * mTLS Security Configuration
 */
@ConfigMapping(prefix = "silat.security.mtls")
public interface MtlsSecurityConfig {

    /**
     * Enable/Disable mTLS support
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Header name for client common name (CN)
     */
    @WithName("header-name")
    @WithDefault("X-SSL-Client-CN")
    String headerName();

    /**
     * Header name for tenant ID
     */
    @WithName("tenant-id-header")
    @WithDefault("X-Tenant-ID")
    String tenantIdHeader();

    /**
     * Default roles assigned to mTLS authenticated users
     */
    @WithName("default-roles")
    @WithDefault("user")
    List<String> defaultRoles();
}
