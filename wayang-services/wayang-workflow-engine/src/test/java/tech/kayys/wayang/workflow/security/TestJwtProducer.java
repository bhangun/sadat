package tech.kayys.wayang.workflow.security;

import io.smallrye.jwt.auth.principal.JWTParser;
import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.mockito.Mockito;

@ApplicationScoped
public class TestJwtProducer {

    @Produces
    @Mock
    @ApplicationScoped
    public JWTParser createMockJwtParser() {
        return Mockito.mock(JWTParser.class);
    }
}
