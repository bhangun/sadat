package tech.kayys.wayang.schema.governance;

import java.util.Arrays;
import java.util.List;

public class AuthConfig {
    private String scheme;
    private String credentialsRef;
    private List<String> scopes;

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        List<String> validSchemes = Arrays.asList("none", "basic", "bearer", "oauth2",
                "apiKey", "aws_sigv4");
        if (!validSchemes.contains(scheme)) {
            throw new IllegalArgumentException("Invalid auth scheme: " + scheme);
        }
        this.scheme = scheme;
    }

    public String getCredentialsRef() {
        return credentialsRef;
    }

    public void setCredentialsRef(String credentialsRef) {
        this.credentialsRef = credentialsRef;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }
}
