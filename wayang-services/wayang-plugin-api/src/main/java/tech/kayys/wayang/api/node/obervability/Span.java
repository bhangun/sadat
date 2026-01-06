package tech.kayys.wayang.api.node.obervability;

/**
 * No-op Span used only for satisfying tracing references.
 */
public final class Span {

    private String name;
    private Context parent;

    public Span() {
        this("noop", Context.root());
    }

    public Span(String name, Context parent) {
        this.name = name;
        this.parent = parent;
    }

    public void end() {
        // no-op
    }

    public Span withTag(String key, String value) {
        return this;
    }

    public void finish() {
        // no-op
    }

    public Span setName(String name) {
        this.name = name;
        return this;
    }

    public Span setParent(Context parent) {
        this.parent = parent;
        return this;
    }

    public String getName() {
        return name;
    }

    public Context getParent() {
        return parent;
    }
}