package tech.kayys.wayang.api.node;

/**
 * Simple generic emitter shim used by `ProvenanceLogger`.
 */
public interface Emitter<T> {
    void send(T payload);
}
