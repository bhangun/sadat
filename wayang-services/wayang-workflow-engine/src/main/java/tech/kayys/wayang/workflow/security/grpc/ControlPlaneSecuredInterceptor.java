package tech.kayys.wayang.workflow.security.grpc;

import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import tech.kayys.wayang.workflow.security.annotations.ControlPlaneSecured;

@ControlPlaneSecured
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class ControlPlaneSecuredInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        // gRPC Context is thread-local here
        Context currentContext = Context.current();
        String principalType = AuthenticationInterceptor.PRINCIPAL_TYPE_KEY.get(currentContext);

        // Allow USER_JWT or SERVICE_ACCOUNT
        if (AuthenticationInterceptor.TYPE_USER_JWT.equals(principalType) ||
                AuthenticationInterceptor.TYPE_SERVICE_ACCOUNT.equals(principalType)) {
            return context.proceed();
        }

        // Reject if accessing Control Plane with Execution Token (or Anonymous)
        throw new StatusRuntimeException(
                Status.PERMISSION_DENIED
                        .withDescription("Control Plane access requires valid User or Service Account"));
    }
}
