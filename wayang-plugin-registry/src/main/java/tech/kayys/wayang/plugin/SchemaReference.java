package tech.kayys.wayang.plugin;

/**
 * Schema reference - can be inline JSON or file path
 */
public class SchemaReference {
    public SchemaType type; // INLINE, FILE, URL
    public String content; // Schema content or path

    public enum SchemaType {
        INLINE, // Inline JSON
        FILE, // File path in JAR
        URL // External URL
    }
}
