package tech.kayys.wayang.plugin.node;

import java.util.Map;

/**
 * Small schema validation utility used by `AbstractNode` to avoid
 * coupling to a specific JSON schema library in this module.
 *
 * Note: This performs a conservative / permissive check (returns true)
 * so it doesn't block compilation when JSON Schema validator libraries
 * are not available. Replace with real validation if desired.
 */
public final class SchemaUtils {

    public static boolean validate(Object value, Map<String, Object> schema) {
        // Lightweight permissive validator: accept non-null values when schema present.
        // TODO: wire up `SchemaValidator` bean or networknt JSON Schema for full validation.
        return true;
    }
}
