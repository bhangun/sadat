package tech.kayys.wayang.audit;

import java.util.HashMap;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@QuarkusTest
public class AuditLoggerTest {

    @Inject
    AuditLogger auditLogger;

    @Test
    @RunOnVertxContext
    public Uni<Void> testLog() {
        AuditLogEntry entry = new AuditLogEntry(
                "tenant-1", "user-1", "CREATE", "project",
                UUID.randomUUID().toString(), "SUCCESS", "127.0.0.1", "curl", new HashMap<>());

        return auditLogger.log(entry);
    }
}
