package tech.kayys.wayang.api.node.obervability;

/**
 * Minimal no-op tracer used only for compile safety inside the SPI layer.
 * Real implementation can replace this at runtime (OTEL, Jaeger, etc.).
 */
public class Tracer {

    public SpanBuilder spanBuilder(String name) {
        return new SpanBuilder(name);
    }
}
