package tech.kayys.wayang.mcp.service;

import io.quarkus.test.junit.QuarkusTest;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import tech.kayys.wayang.mcp.dto.CapabilityLevel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ToolCapabilityAnalyzerTest {

    private ToolCapabilityAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new ToolCapabilityAnalyzer();
    }

    @Test
    void testClassifyGetAsReadOnly() {
        Operation operation = new Operation();
        operation.setOperationId("getUser");

        CapabilityLevel level = analyzer.analyze(
                PathItem.HttpMethod.GET,
                operation,
                "/users/{id}");

        assertEquals(CapabilityLevel.READ_ONLY, level);
    }

    @Test
    void testClassifyPostAsStateChanging() {
        Operation operation = new Operation();
        operation.setOperationId("createUser");

        CapabilityLevel level = analyzer.analyze(
                PathItem.HttpMethod.POST,
                operation,
                "/users");

        assertEquals(CapabilityLevel.STATE_CHANGING, level);
    }

    @Test
    void testClassifyPutAsStateChanging() {
        Operation operation = new Operation();
        operation.setOperationId("updateUser");

        CapabilityLevel level = analyzer.analyze(
                PathItem.HttpMethod.PUT,
                operation,
                "/users/{id}");

        assertEquals(CapabilityLevel.STATE_CHANGING, level);
    }

    @Test
    void testClassifyPatchAsStateChanging() {
        Operation operation = new Operation();
        operation.setOperationId("modifyUser");

        CapabilityLevel level = analyzer.analyze(
                PathItem.HttpMethod.PATCH,
                operation,
                "/users/{id}");

        assertEquals(CapabilityLevel.STATE_CHANGING, level);
    }

    @Test
    void testClassifyDeleteAsDestructive() {
        Operation operation = new Operation();
        operation.setOperationId("deleteUser");

        CapabilityLevel level = analyzer.analyze(
                PathItem.HttpMethod.DELETE,
                operation,
                "/users/{id}");

        assertEquals(CapabilityLevel.DESTRUCTIVE, level);
    }

    @Test
    void testDetectStateChangingByOperationId() {
        String[] stateChangingOps = {
                "createUser", "updateUser", "deleteUser", "modifyUser",
                "changePassword", "setConfig", "configureSettings",
                "activateAccount", "deactivateAccount", "enableFeature",
                "disableFeature", "installPlugin", "uninstallPlugin",
                "deployApp", "undeployApp", "startService", "stopService",
                "restartService", "shutdownSystem", "terminateProcess",
                "suspendUser", "resumeUser"
        };

        for (String opId : stateChangingOps) {
            Operation operation = new Operation();
            operation.setOperationId(opId);

            CapabilityLevel level = analyzer.analyze(
                    PathItem.HttpMethod.POST,
                    operation,
                    "/api/action");

            assertEquals(CapabilityLevel.STATE_CHANGING, level,
                    "Operation '" + opId + "' should be classified as STATE_CHANGING");
        }
    }

    @Test
    void testDetectStateChangingByPath() {
        String[] stateChangingPaths = {
                "/settings", "/config", "/preferences", "/profile",
                "/account", "/user", "/admin", "/system", "/control"
        };

        for (String path : stateChangingPaths) {
            Operation operation = new Operation();
            operation.setOperationId("updateData");

            CapabilityLevel level = analyzer.analyze(
                    PathItem.HttpMethod.POST,
                    operation,
                    path);

            assertEquals(CapabilityLevel.STATE_CHANGING, level,
                    "Path '" + path + "' should be classified as STATE_CHANGING");
        }
    }

    @Test
    void testNonStateChangingPostAsDataManipulation() {
        Operation operation = new Operation();
        operation.setOperationId("searchUsers");

        CapabilityLevel level = analyzer.analyze(
                PathItem.HttpMethod.POST,
                operation,
                "/search");

        assertEquals(CapabilityLevel.DATA_MANIPULATION, level);
    }

    @Test
    void testCaseInsensitiveOperationIdMatching() {
        Operation operation = new Operation();
        operation.setOperationId("CreateUser"); // Mixed case

        CapabilityLevel level = analyzer.analyze(
                PathItem.HttpMethod.POST,
                operation,
                "/users");

        assertEquals(CapabilityLevel.STATE_CHANGING, level);
    }

    @Test
    void testCaseInsensitivePathMatching() {
        Operation operation = new Operation();
        operation.setOperationId("updateData");

        CapabilityLevel level = analyzer.analyze(
                PathItem.HttpMethod.POST,
                operation,
                "/Settings" // Mixed case
        );

        assertEquals(CapabilityLevel.STATE_CHANGING, level);
    }

    @Test
    void testNullOperationId() {
        Operation operation = new Operation();
        operation.setOperationId(null);

        CapabilityLevel level = analyzer.analyze(
                PathItem.HttpMethod.POST,
                operation,
                "/settings");

        // Should fall back to path-based detection
        assertEquals(CapabilityLevel.STATE_CHANGING, level);
    }
}
