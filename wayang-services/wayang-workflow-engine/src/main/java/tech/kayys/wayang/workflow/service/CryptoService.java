package tech.kayys.wayang.workflow.service;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Crypto service for signing/hashing.
 */
@ApplicationScoped
class CryptoService {
    // Placeholder for cryptographic operations
    public String sign(String data) {
        // Implement signing logic
        return "signature";
    }

    public boolean verify(String data, String signature) {
        // Implement verification logic
        return true;
    }
}
