package tech.kayys.wayang.agent.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.PreparedQuery;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.model.Message;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
public class PostgresVectorStoreTest {

    @Inject
    PostgresVectorStore store;

    @InjectMock
    PgPool pgPool;

    @Test
    void testStore() {
        Message message = Message.user("hello");
        float[] embedding = new float[1536];

        PreparedQuery<RowSet<Row>> query = mock(PreparedQuery.class);
        when(pgPool.preparedQuery(anyString())).thenReturn(query);
        when(query.execute(any())).thenReturn(Uni.createFrom().item(mock(RowSet.class)));

        store.store("session-1", "tenant-1", message, embedding)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem();
    }
}
