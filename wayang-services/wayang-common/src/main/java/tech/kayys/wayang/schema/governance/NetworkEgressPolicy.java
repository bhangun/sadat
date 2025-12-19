package tech.kayys.wayang.schema.governance;

import java.util.List;

public class NetworkEgressPolicy {
    private List<String> allowedDomains;
    private List<String> deniedDomains;

    public List<String> getAllowedDomains() {
        return allowedDomains;
    }

    public void setAllowedDomains(List<String> allowedDomains) {
        this.allowedDomains = allowedDomains;
    }

    public List<String> getDeniedDomains() {
        return deniedDomains;
    }

    public void setDeniedDomains(List<String> deniedDomains) {
        this.deniedDomains = deniedDomains;
    }
}
