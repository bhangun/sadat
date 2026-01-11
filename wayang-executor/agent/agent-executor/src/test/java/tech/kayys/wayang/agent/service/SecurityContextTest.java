package tech.kayys.wayang.agent.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.dto.AgentPrincipal;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class SecurityContextTest {

    @Inject
    SecurityContext securityContext;

    @Test
    void testSecurityContext() {
        AgentPrincipal principal = new AgentPrincipal(
                "user-1",
                "tenant-1",
                List.of("admin"),
                List.of("tool:weather"));

        securityContext.setPrincipal(principal);

        assertEquals("user-1", securityContext.getCurrentUserId());
        assertEquals("tenant-1", securityContext.getCurrentTenantId());
        assertTrue(securityContext.hasRole("admin"));
        assertTrue(securityContext.hasPermission("tool:weather"));
        assertFalse(securityContext.hasPermission("tool:other"));

        securityContext.clear();
        assertNull(securityContext.getPrincipal());
    }
}
