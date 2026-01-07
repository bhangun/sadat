package tech.kayys.wayang.organization.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Embeddable;

/**
 * Organization settings
 */
@Embeddable
public class OrganizationSettings {
    public String timezone = "UTC";
    public String currency = "USD";
    public String language = "en";
    public boolean allowApiAccess = true;
    public boolean enableAuditLogs = true;
    public boolean enforceMfa = false;
    public boolean allowResourceSharing = true;
    public int sessionTimeoutMinutes = 60;
    public List<String> allowedIpRanges = new ArrayList<>();
}

