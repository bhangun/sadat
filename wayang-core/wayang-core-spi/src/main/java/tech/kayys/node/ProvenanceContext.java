package tech.kayys.wayang.plugin.node;

/**
 * Simple provenance context shim used by SPI compile-time.
 */
public interface ProvenanceContext {
    ProvenanceService getService();
}
