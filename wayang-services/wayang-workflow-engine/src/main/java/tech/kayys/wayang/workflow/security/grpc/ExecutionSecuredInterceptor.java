package tech.kayys.wayang.workflow.security.grpc;

import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import tech.kayys.wayang.workflow.security.annotations.ExecutionSecured;

@ExecutionSecured
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class ExecutionSecuredInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Context currentContext = Context.current();
        String principalType = AuthenticationInterceptor.PRINCIPAL_TYPE_KEY.get(currentContext);

        // Allow EXECUTION_TOKEN
        if (AuthenticationInterceptor.TYPE_EXECUTION_TOKEN.equals(principalType)) {
            return context.proceed();
        }

        // Reject if accessing Execution Plane with User Token (Isolation)
        throw new StatusRuntimeException(
                Status.PERMISSION_DENIED.withDescription("Execution Plane access requires valid Execution Token"));
    }
}
