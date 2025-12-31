package tech.kayys.wayang.workflow.security.annotations;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Security annotation for Execution Plane services.
 * Enforces that the request is authenticated via a signed Execution Context
 * Token.
 * Rejects standard USER JWTs.
 */
@InterceptorBinding
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ExecutionSecured {
}
