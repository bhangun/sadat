package tech.kayys.wayang.plugin.node.obervability;

/**
 * Minimal Context placeholder.
 * Mirrors OpenTelemetry Context enough for compile-time safety.
 */
public final class Context {

    private static final Context ROOT = new Context();

    private Context() {
    }

    public static Context current() {
        return ROOT;
    }

    public static Context root() {
        return ROOT;
    }
}
