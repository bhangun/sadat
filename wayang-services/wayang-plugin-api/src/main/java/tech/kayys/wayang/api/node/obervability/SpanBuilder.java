package tech.kayys.wayang.api.node.obervability;

/**
 * No-op SpanBuilder that mirrors the structure of OpenTelemetry SpanBuilder.
 */
public final class SpanBuilder {

    private final String name;
    private Context parent;

    public SpanBuilder(String name) {
        this.name = name;
    }

    public SpanBuilder setParent(Context parent) {
        this.parent = parent;
        return this;
    }

    public Span startSpan() {
        return new Span(name, parent);
    }
}