package tech.kayys.wayang;

import io.quarkus.test.Mock;
import io.vertx.mutiny.sqlclient.Pool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.mockito.Mockito;

@ApplicationScoped
public class GlobalTestMocks {

    @Produces
    @Mock
    @ApplicationScoped
    public Pool mockPool() {
        return Mockito.mock(Pool.class);
    }
}
