package tech.kayys.wayang.workflow.security.grpc;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.quarkus.grpc.GlobalInterceptor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.workflow.security.service.ExecutionContextService;
import tech.kayys.wayang.workflow.security.model.ExecutionContext;

/**
 * First interceptor in the chain.
 * Handles Authentication for:
 * 1. User JWTs (OIDC)
 * 2. Service Accounts (OIDC/Client Credentials)
 * 3. Execution Context Tokens (Runtime)
 */
@ApplicationScoped
@GlobalInterceptor
public class AuthenticationInterceptor implements ServerInterceptor {

    private static final Logger LOG = Logger.getLogger(AuthenticationInterceptor.class);
    private static final Metadata.Key<String> AUTHORIZATION = Metadata.Key.of("Authorization",
            Metadata.ASCII_STRING_MARSHALLER);

    public static final Context.Key<ExecutionContext> EXECUTION_CONTEXT_KEY = Context.key("execution-context");
    public static final Context.Key<String> PRINCIPAL_TYPE_KEY = Context.key("principal-type");

    public static final String TYPE_EXECUTION_TOKEN = "EXECUTION_TOKEN";
    public static final String TYPE_USER_JWT = "USER_JWT";
    public static final String TYPE_SERVICE_ACCOUNT = "SERVICE_ACCOUNT";

    @Inject
    ExecutionContextService executionContextService;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String authHeader = headers.get(AUTHORIZATION);
        Context context = Context.current();

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                // Optimistically try to validate as Execution Context Token
                ExecutionContext executionContext = executionContextService.validateContext(token)
                        .await().indefinitely();

                context = context.withValue(EXECUTION_CONTEXT_KEY, executionContext)
                        .withValue(PRINCIPAL_TYPE_KEY, TYPE_EXECUTION_TOKEN);

                LOG.debugf("Authenticated request with Execution Context Token: %s", executionContext.runId());

            } catch (Exception e) {
                // Not a valid execution token.
                // It must be a standard principal (User or Service Account).
                // Distinction between User and Service Account often lives in the claims (e.g.
                // clientId vs preferred_username or 'typ' claim).
                // We'll tag it generically as USER_JWT for now, but downstream we can refine to
                // SERVICE_ACCOUNT in the TenantContext or Service logic.

                // For proper differentiation, we might parse the JWT claims here lightly
                // (Service Account often has 'azp' or specific client scopes).
                // Or we rely on Quarkus SecurityIdentity which will be populated later.

                // Let's set it as USER_JWT (Standard) and let standard auth mechanisms validate
                // it later.
                // We assume OIDC interceptors run or we access SecurityIdentity.

                context = context.withValue(PRINCIPAL_TYPE_KEY, TYPE_USER_JWT);
                LOG.debug("Token failed execution context validation, assuming User/Service JWT", e);
            }
        }

        return Contexts.interceptCall(context, call, headers, next);
    }
}
