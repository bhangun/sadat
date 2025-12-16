package tech.kayys.node;

public enum SandboxLevel {
    TRUSTED,        // In-process JVM classloader
    SEMI_TRUSTED,   // Isolated classloader + SecurityManager
    UNTRUSTED       // WASM or container sidecar
}
