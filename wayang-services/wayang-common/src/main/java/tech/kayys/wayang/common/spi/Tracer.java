package tech.kayys.wayang.common.spi;

import java.util.UUID;

public interface Tracer {
    static SpanBuilder spanBuilder(String name) {
        return new NoOpSpanBuilder();
    }

    interface SpanBuilder {
        SpanBuilder withTag(String key, String value);
        SpanBuilder withTag(String key, UUID value);
        Span start();
    }

    class NoOpSpanBuilder implements SpanBuilder {
        @Override public SpanBuilder withTag(String key, String value) { return this; }
        @Override public SpanBuilder withTag(String key, UUID value) { return this; }
        @Override public Span start() { return () -> {}; }
    }
}
