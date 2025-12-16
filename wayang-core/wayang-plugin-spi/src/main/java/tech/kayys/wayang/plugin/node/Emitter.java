package tech.kayys.wayang.plugin.node;

/**
 * Simple generic emitter shim used by `ProvenanceLogger`.
 */
public interface Emitter<T> {
    void send(T payload);
}
