package tech.kayys.wayang.agent.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.dto.AuditLogEntry;
import tech.kayys.wayang.agent.repository.AuditLogRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class SecurityAuditLoggerTest {

    @Inject
    SecurityAuditLogger logger;

    @InjectMock
    AuditLogRepository repository;

    @Test
    void testLogAuthentication() {
        when(repository.save(any(AuditLogEntry.class))).thenReturn(Uni.createFrom().voidItem());

        logger.logAuthentication("user-1", "tenant-1", true, "127.0.0.1");

        verify(repository).save(any(AuditLogEntry.class));
    }

    @Test
    void testLogAuthorization() {
        when(repository.save(any(AuditLogEntry.class))).thenReturn(Uni.createFrom().voidItem());

        logger.logAuthorization("user-1", "tenant-1", "resource", "read", true);

        verify(repository).save(any(AuditLogEntry.class));
    }

    @Test
    void testLogDataAccess() {
        when(repository.save(any(AuditLogEntry.class))).thenReturn(Uni.createFrom().voidItem());

        logger.logDataAccess("user-1", "tenant-1", "file", "id-1", "read");

        verify(repository).save(any(AuditLogEntry.class));
    }
}
