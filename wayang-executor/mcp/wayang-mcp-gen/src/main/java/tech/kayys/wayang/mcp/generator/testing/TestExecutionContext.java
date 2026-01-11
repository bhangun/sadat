package tech.kayys.wayang.mcp.generator.testing;

public class TestExecutionContext {
    private final String baseUrl;
    private final String adminToken;
    private final String userToken;
    private final String guestToken;
    private final String validToken;
    private final String regularUserToken;
    private final String currentUserId;

    private TestExecutionContext(Builder builder) {
        this.baseUrl = builder.baseUrl;
        this.adminToken = builder.adminToken;
        this.userToken = builder.userToken;
        this.guestToken = builder.guestToken;
        this.validToken = builder.validToken;
        this.regularUserToken = builder.regularUserToken;
        this.currentUserId = builder.currentUserId;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getAdminToken() {
        return adminToken;
    }

    public String getUserToken() {
        return userToken;
    }

    public String getGuestToken() {
        return guestToken;
    }

    public String getValidToken() {
        return validToken;
    }

    public String getRegularUserToken() {
        return regularUserToken;
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String baseUrl;
        private String adminToken;
        private String userToken;
        private String guestToken;
        private String validToken;
        private String regularUserToken;
        private String currentUserId;

        public Builder withBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder withAdminToken(String adminToken) {
            this.adminToken = adminToken;
            return this;
        }

        public Builder withUserToken(String userToken) {
            this.userToken = userToken;
            return this;
        }

        public Builder withGuestToken(String guestToken) {
            this.guestToken = guestToken;
            return this;
        }

        public Builder withValidToken(String validToken) {
            this.validToken = validToken;
            return this;
        }

        public Builder withRegularUserToken(String regularUserToken) {
            this.regularUserToken = regularUserToken;
            return this;
        }

        public Builder withCurrentUserId(String currentUserId) {
            this.currentUserId = currentUserId;
            return this;
        }

        public TestExecutionContext build() {
            return new TestExecutionContext(this);
        }
    }
}