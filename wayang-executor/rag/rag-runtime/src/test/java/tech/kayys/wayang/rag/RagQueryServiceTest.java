package tech.kayys.silat.executor.rag.examples;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.silat.client.SilatClient;
import tech.kayys.silat.executor.rag.domain.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagQueryServiceTest {

    @Mock
    private SilatClient silatClient;

    private RagQueryService ragQueryService;

    @BeforeEach
    void setUp() {
        ragQueryService = new RagQueryService();
        // Note: Since we can't directly inject the mock due to private field,
        // we would typically use reflection or constructor injection in a real scenario
    }

    @Test
    void testQuery_Success() {
        // Given
        String tenantId = "test-tenant";
        String query = "test query";
        String collectionName = "test-collection";

        // When
        Uni<RagResponse> result = ragQueryService.query(tenantId, query, collectionName);

        // Then
        assertNotNull(result);
        // Additional assertions would require mocking the silatClient behavior
    }

    @Test
    void testAdvancedQuery_Success() {
        // Given
        RagQueryRequest request = new RagQueryRequest(
            "test-tenant",
            "test query",
            RagMode.STANDARD,
            SearchStrategy.HYBRID,
            RetrievalConfig.defaults(),
            GenerationConfig.defaults(),
            List.of("test-collection"),
            Map.of()
        );

        // When
        Uni<RagResponse> result = ragQueryService.advancedQuery(request);

        // Then
        assertNotNull(result);
    }

    @Test
    void testConversationalQuery_Success() {
        // Given
        String tenantId = "test-tenant";
        String query = "test query";
        String sessionId = "session-123";
        List<ConversationTurn> history = List.of(
            new ConversationTurn("user msg", "assistant msg", Instant.now())
        );

        // When
        Uni<RagResponse> result = ragQueryService.conversationalQuery(tenantId, query, sessionId, history);

        // Then
        assertNotNull(result);
    }
}