package tech.kayys.silat.executor.rag.examples;

import io.smallrye.mutiny.Uni;
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

@ExtendWith(MockitoExtension.class)
class IntegrationTest {

    @Mock
    private SilatClient silatClient;

    @Test
    void testCompleteRagFlow() {
        // This is a high-level integration test demonstrating how components work together
        
        // Given
        String tenantId = "integration-test-tenant";
        String query = "What is the meaning of life?";
        
        // Create services (in a real test, we'd properly mock dependencies)
        DocumentIngestionService ingestionService = new DocumentIngestionService();
        RagQueryService queryService = new RagQueryService();
        
        // Create a sample document source
        DocumentSource source = new DocumentSource(
            SourceType.TEXT,
            null,
            "Life is a characteristic that distinguishes physical entities that have biological processes from those that do not.",
            Map.of("topic", "philosophy")
        );
        
        // When - Test document ingestion
        Uni<IngestResult> ingestResult = ingestionService.ingestTextDocuments(
            tenantId,
            List.of(source.content()),
            source.metadata(),
            ChunkingConfig.defaults()
        );
        
        // Then - Verify ingestion result structure
        assertNotNull(ingestResult);
        
        // When - Test query creation
        RagQueryRequest request = new RagQueryRequest(
            tenantId,
            query,
            RagMode.STANDARD,
            SearchStrategy.HYBRID,
            RetrievalConfig.defaults(),
            GenerationConfig.defaults(),
            List.of("philosophy-docs"),
            Map.of()
        );
        
        // Then - Verify request structure
        assertEquals(tenantId, request.tenantId());
        assertEquals(query, request.query());
        assertEquals(RagMode.STANDARD, request.ragMode());
        
        // When - Test conversation turn creation
        ConversationTurn turn = new ConversationTurn(
            query,
            "The meaning of life is subjective and varies between individuals and cultures.",
            Instant.now()
        );
        
        // Then - Verify conversation turn structure
        assertEquals(query, turn.userMessage());
        assertFalse(turn.assistantMessage().isEmpty());
        assertNotNull(turn.timestamp());
    }
    
    @Test
    void testRecordEqualityAndHashCode() {
        // Test equality and hash code for records
        
        // IngestResult
        IngestResult result1 = new IngestResult(true, 1, 10, 100L, "Success");
        IngestResult result2 = new IngestResult(true, 1, 10, 100L, "Success");
        IngestResult result3 = new IngestResult(false, 1, 10, 100L, "Success");
        
        assertEquals(result1, result2);
        assertNotEquals(result1, result3);
        assertEquals(result1.hashCode(), result2.hashCode());
        
        // DocumentSource
        DocumentSource source1 = new DocumentSource(SourceType.PDF, "/doc1.pdf", null, Map.of("key", "value"));
        DocumentSource source2 = new DocumentSource(SourceType.PDF, "/doc1.pdf", null, Map.of("key", "value"));
        DocumentSource source3 = new DocumentSource(SourceType.TEXT, "/doc1.pdf", null, Map.of("key", "value"));
        
        assertEquals(source1, source2);
        assertNotEquals(source1, source3);
        assertEquals(source1.hashCode(), source2.hashCode());
        
        // RagQueryRequest
        RagQueryRequest request1 = new RagQueryRequest(
            "tenant1", "query1", RagMode.STANDARD, SearchStrategy.HYBRID,
            RetrievalConfig.defaults(), GenerationConfig.defaults(),
            List.of("col1"), Map.of("filter", "value")
        );
        RagQueryRequest request2 = new RagQueryRequest(
            "tenant1", "query1", RagMode.STANDARD, SearchStrategy.HYBRID,
            RetrievalConfig.defaults(), GenerationConfig.defaults(),
            List.of("col1"), Map.of("filter", "value")
        );
        RagQueryRequest request3 = new RagQueryRequest(
            "tenant2", "query1", RagMode.STANDARD, SearchStrategy.HYBRID,
            RetrievalConfig.defaults(), GenerationConfig.defaults(),
            List.of("col1"), Map.of("filter", "value")
        );
        
        assertEquals(request1, request2);
        assertNotEquals(request1, request3);
        assertEquals(request1.hashCode(), request2.hashCode());
        
        // ConversationTurn
        Instant now = Instant.now();
        ConversationTurn turn1 = new ConversationTurn("msg1", "msg2", now);
        ConversationTurn turn2 = new ConversationTurn("msg1", "msg2", now);
        ConversationTurn turn3 = new ConversationTurn("msg1", "msg3", now);
        
        assertEquals(turn1, turn2);
        assertNotEquals(turn1, turn3);
        assertEquals(turn1.hashCode(), turn2.hashCode());
    }
}