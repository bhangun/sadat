package tech.kayys.wayang.mcp.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tech.kayys.wayang.mcp.dto.TenantContext;
import tech.kayys.wayang.mcp.TestFixtures;
import tech.kayys.wayang.mcp.domain.AuthProfile;
import tech.kayys.wayang.mcp.security.VaultSecretManager;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@QuarkusTest
class AuthProfileResourceTest {

        @InjectMock
        TenantContext tenantContext;

        @InjectMock
        VaultSecretManager vaultManager;

        @Test
        void testCreateAuthProfileSuccess() {
                // Mock tenant context
                Mockito.when(tenantContext.getCurrentTenantId())
                                .thenReturn(TestFixtures.TEST_TENANT_ID);

                // Mock vault storage
                Mockito.when(vaultManager.storeSecret(anyString(), anyString()))
                                .thenReturn(Uni.createFrom().voidItem());

                given()
                                .contentType(ContentType.JSON)
                                .body(Map.of(
                                                "profileName", "Test Auth Profile",
                                                "authType", "BEARER",
                                                "description", "Test description",
                                                "location", "HEADER",
                                                "paramName", "Authorization",
                                                "scheme", "Bearer",
                                                "secretValue", "test-secret-123"))
                                .when()
                                .post("/api/v1/mcp/auth-profiles")
                                .then()
                                .statusCode(201)
                                .body("profileName", equalTo("Test Auth Profile"))
                                .body("authType", equalTo("BEARER"))
                                .body("enabled", equalTo(true))
                                .body("profileId", notNullValue());
        }

        @Test
        void testCreateAuthProfileValidation() {
                // Mock tenant context
                Mockito.when(tenantContext.getCurrentTenantId())
                                .thenReturn(TestFixtures.TEST_TENANT_ID);

                // Missing required fields
                given()
                                .contentType(ContentType.JSON)
                                .body(Map.of(
                                                "profileName", "Test Auth Profile"
                                // Missing other required fields
                                ))
                                .when()
                                .post("/api/v1/mcp/auth-profiles")
                                .then()
                                .statusCode(anyOf(equalTo(400), equalTo(500))); // Validation error
        }

        @Test
        void testCreateAuthProfileVaultFailure() {
                // Mock tenant context
                Mockito.when(tenantContext.getCurrentTenantId())
                                .thenReturn(TestFixtures.TEST_TENANT_ID);

                // Mock vault storage failure
                Mockito.when(vaultManager.storeSecret(anyString(), anyString()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Vault error")));

                given()
                                .contentType(ContentType.JSON)
                                .body(Map.of(
                                                "profileName", "Test Auth Profile",
                                                "authType", "BEARER",
                                                "description", "Test description",
                                                "location", "HEADER",
                                                "paramName", "Authorization",
                                                "scheme", "Bearer",
                                                "secretValue", "test-secret-123"))
                                .when()
                                .post("/api/v1/mcp/auth-profiles")
                                .then()
                                .statusCode(500);
        }

        @Test
        void testListAuthProfiles() {
                // Mock tenant context
                Mockito.when(tenantContext.getCurrentTenantId())
                                .thenReturn(TestFixtures.TEST_TENANT_ID);

                // Note: This test will return empty list as we're not setting up database
                given()
                                .when()
                                .get("/api/v1/mcp/auth-profiles")
                                .then()
                                .statusCode(200)
                                .body("$", instanceOf(List.class));
        }

        @Test
        void testCreateApiKeyAuthProfile() {
                // Mock tenant context
                Mockito.when(tenantContext.getCurrentTenantId())
                                .thenReturn(TestFixtures.TEST_TENANT_ID);

                // Mock vault storage
                Mockito.when(vaultManager.storeSecret(anyString(), anyString()))
                                .thenReturn(Uni.createFrom().voidItem());

                given()
                                .contentType(ContentType.JSON)
                                .body(Map.of(
                                                "profileName", "API Key Auth",
                                                "authType", "API_KEY",
                                                "description", "API Key authentication",
                                                "location", "QUERY",
                                                "paramName", "api_key",
                                                "scheme", "",
                                                "secretValue", "my-api-key-123"))
                                .when()
                                .post("/api/v1/mcp/auth-profiles")
                                .then()
                                .statusCode(201)
                                .body("profileName", equalTo("API Key Auth"))
                                .body("authType", equalTo("API_KEY"));
        }

        @Test
        void testCreateBasicAuthProfile() {
                // Mock tenant context
                Mockito.when(tenantContext.getCurrentTenantId())
                                .thenReturn(TestFixtures.TEST_TENANT_ID);

                // Mock vault storage
                Mockito.when(vaultManager.storeSecret(anyString(), anyString()))
                                .thenReturn(Uni.createFrom().voidItem());

                given()
                                .contentType(ContentType.JSON)
                                .body(Map.of(
                                                "profileName", "Basic Auth",
                                                "authType", "BASIC",
                                                "description", "Basic authentication",
                                                "location", "HEADER",
                                                "paramName", "Authorization",
                                                "scheme", "Basic",
                                                "secretValue", "dXNlcjpwYXNz" // base64 encoded user:pass
                                ))
                                .when()
                                .post("/api/v1/mcp/auth-profiles")
                                .then()
                                .statusCode(201)
                                .body("profileName", equalTo("Basic Auth"))
                                .body("authType", equalTo("BASIC"));
        }
}
