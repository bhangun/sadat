package tech.kayys.wayang.schema.agent;

import java.util.List;

public class A2AConfig {
    private String endpoint;
    private String protocolVersion;
    private String authRef;
    private List<String> capabilities;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public String getAuthRef() {
        return authRef;
    }

    public void setAuthRef(String authRef) {
        this.authRef = authRef;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }
}
