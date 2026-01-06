package tech.kayys.wayang.plugin.node;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a node within a multi-node plugin
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(Nodes.class)
public @interface Node {

    /**
     * Node type identifier
     */
    String type();

    /**
     * Node label
     */
    String label();

    /**
     * Category
     */
    String category() default "";

    /**
     * Subcategory
     */
    String subCategory() default "";

    /**
     * Description
     */
    String description() default "";

    /**
     * Icon
     */
    String icon() default "box";

    /**
     * Color (hex)
     */
    String color() default "#6B7280";

    /**
     * Config schema (JSON Schema as string or resource path)
     */
    String configSchema() default "";

    /**
     * Input schema
     */
    String inputSchema() default "";

    /**
     * Output schema
     */
    String outputSchema() default "";

    /**
     * Executor ID (if different from plugin default)
     */
    String executorId() default "";

    /**
     * Widget ID
     */
    String widgetId() default "";
}
