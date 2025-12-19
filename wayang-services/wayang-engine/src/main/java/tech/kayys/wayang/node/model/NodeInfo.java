package tech.kayys.wayang.node.model;

/**
 * Node info annotation for built-in nodes.
 */
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
@interface NodeInfo {
    String name();

    String[] capabilities() default {};

    String category() default "integration";
}
