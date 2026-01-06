package tech.kayys.wayang.plugin.multi;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import tech.kayys.wayang.plugin.execution.ExecutionMode;

/**
 * Marks an executor that handles multiple node types
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MultiNodeExecutor {

    /**
     * Executor ID
     */
    String executorId();

    /**
     * Node types this executor handles
     */
    String[] nodeTypes();

    /**
     * Execution mode
     */
    ExecutionMode mode() default ExecutionMode.SYNC;

    /**
     * Protocols
     */
    String[] protocols() default { "REST" };
}