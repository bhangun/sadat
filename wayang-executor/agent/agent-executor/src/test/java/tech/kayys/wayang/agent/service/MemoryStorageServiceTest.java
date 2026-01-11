package tech.kayys.wayang.agent.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.model.Message;
import tech.kayys.wayang.agent.repository.MessageRepository;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class MemoryStorageServiceTest {

    @Inject
    MemoryStorageService service;

    @InjectMock
    MessageRepository repository;

    @Test
    void testLoadMessages() {
        String sessionId = "session-1";
        String tenantId = "tenant-1";
        List<Message> messages = List.of(Message.user("hello"));

        when(repository.findBySession(sessionId, tenantId)).thenReturn(Uni.createFrom().item(messages));

        service.loadMessages(sessionId, tenantId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(messages);

        verify(repository).findBySession(sessionId, tenantId);
    }

    @Test
    void testSaveMessages() {
        String sessionId = "session-1";
        String tenantId = "tenant-1";
        List<Message> messages = List.of(Message.user("hello"));

        when(repository.save(eq(sessionId), eq(tenantId), anyList())).thenReturn(Uni.createFrom().voidItem());

        service.saveMessages(sessionId, tenantId, messages)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem();

        verify(repository).save(eq(sessionId), eq(tenantId), anyList());
    }

    @Test
    void testClearMessages() {
        when(repository.deleteBySession(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

        service.clearMessages("session-1", "tenant-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem();
    }
}
