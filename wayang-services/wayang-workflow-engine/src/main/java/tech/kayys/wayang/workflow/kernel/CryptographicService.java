package tech.kayys.wayang.workflow.kernel;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for cryptographic operations
 */
@ApplicationScoped
public class CryptographicService {

    private static final Logger LOG = LoggerFactory.getLogger(CryptographicService.class);

    @Inject
    ConfigurationService configService;

    private final Map<String, KeyPair> keyPairs = new HashMap<>();
    private final Map<String, Instant> keyExpiry = new HashMap<>();
    private volatile KeyPair currentKeyPair;

    public Uni<String> sign(String data) {
        return Uni.createFrom().deferred(() -> {
            try {
                KeyPair keyPair = getCurrentKeyPair();
                if (keyPair == null) {
                    return Uni.createFrom().failure(
                            new IllegalStateException("No key pair available for signing"));
                }

                Signature signature = Signature.getInstance("SHA256withRSA");
                signature.initSign(keyPair.getPrivate());
                signature.update(data.getBytes(StandardCharsets.UTF_8));

                byte[] digitalSignature = signature.sign();
                return Uni.createFrom().item(Base64.getEncoder().encodeToString(digitalSignature));
            } catch (Exception e) {
                LOG.error("Error signing data", e);
                return Uni.createFrom().failure(e);
            }
        });
    }

    public Uni<Boolean> verify(String data, String signatureBase64) {
        return Uni.createFrom().deferred(() -> {
            try {
                KeyPair keyPair = getCurrentKeyPair();
                if (keyPair == null) {
                    return Uni.createFrom().item(false);
                }

                Signature signature = Signature.getInstance("SHA256withRSA");
                signature.initVerify(keyPair.getPublic());
                signature.update(data.getBytes(StandardCharsets.UTF_8));

                byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
                return Uni.createFrom().item(signature.verify(signatureBytes));
            } catch (Exception e) {
                LOG.error("Error verifying signature", e);
                return Uni.createFrom().item(false);
            }
        });
    }

    public Uni<String> encrypt(String data, String keyId) {
        return Uni.createFrom().deferred(() -> {
            try {
                // For simplicity, using Base64 "encryption"
                // In production, use proper encryption like AES/GCM
                String encoded = Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
                return Uni.createFrom().item("enc:" + keyId + ":" + encoded);
            } catch (Exception e) {
                LOG.error("Error encrypting data", e);
                return Uni.createFrom().failure(e);
            }
        });
    }

    public Uni<String> decrypt(String encryptedData) {
        return Uni.createFrom().deferred(() -> {
            try {
                if (!encryptedData.startsWith("enc:")) {
                    return Uni.createFrom().item(encryptedData); // Not encrypted
                }

                String[] parts = encryptedData.split(":", 3);
                if (parts.length != 3) {
                    return Uni.createFrom().failure(
                            new IllegalArgumentException("Invalid encrypted data format"));
                }

                String keyId = parts[1];
                String encoded = parts[2];

                byte[] decoded = Base64.getDecoder().decode(encoded);
                return Uni.createFrom().item(new String(decoded, StandardCharsets.UTF_8));
            } catch (Exception e) {
                LOG.error("Error decrypting data", e);
                return Uni.createFrom().failure(e);
            }
        });
    }

    public Uni<String> generateHash(String data, String algorithm) {
        return Uni.createFrom().deferred(() -> {
            try {
                MessageDigest digest = MessageDigest.getInstance(algorithm);
                byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));

                // Convert byte array to hex string
                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) {
                        hexString.append('0');
                    }
                    hexString.append(hex);
                }

                return Uni.createFrom().item(hexString.toString());
            } catch (Exception e) {
                LOG.error("Error generating hash", e);
                return Uni.createFrom().failure(e);
            }
        });
    }

    public Uni<Map<String, String>> generateKeyPair(String keyId) {
        return Uni.createFrom().deferred(() -> {
            try {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(2048);
                KeyPair keyPair = keyGen.generateKeyPair();

                // Store key pair
                keyPairs.put(keyId, keyPair);
                keyExpiry.put(keyId, Instant.now().plusSeconds(86400 * 365)); // 1 year

                // Encode keys to strings
                String publicKey = Base64.getEncoder().encodeToString(
                        keyPair.getPublic().getEncoded());
                String privateKey = Base64.getEncoder().encodeToString(
                        keyPair.getPrivate().getEncoded());

                Map<String, String> keys = new HashMap<>();
                keys.put("keyId", keyId);
                keys.put("publicKey", publicKey);
                keys.put("privateKey", privateKey);
                keys.put("algorithm", "RSA");
                keys.put("expiry", keyExpiry.get(keyId).toString());

                LOG.info("Generated key pair for ID: {}", keyId);
                return Uni.createFrom().item(keys);
            } catch (Exception e) {
                LOG.error("Error generating key pair", e);
                return Uni.createFrom().failure(e);
            }
        });
    }

    public Uni<Boolean> rotateKeys() {
        return Uni.createFrom().deferred(() -> {
            try {
                // Generate new key pair
                String newKeyId = "key-" + Instant.now().toEpochMilli();
                return generateKeyPair(newKeyId)
                        .onItem().invoke(keys -> {
                            // Set as current key pair
                            currentKeyPair = keyPairs.get(newKeyId);
                            LOG.info("Keys rotated to new ID: {}", newKeyId);
                        })
                        .map(keys -> true);
            } catch (Exception e) {
                LOG.error("Error rotating keys", e);
                return Uni.createFrom().item(false);
            }
        });
    }

    public Uni<Void> cleanupExpiredKeys() {
        return Uni.createFrom().deferred(() -> {
            Instant now = Instant.now();
            keyExpiry.entrySet().removeIf(entry -> {
                if (entry.getValue().isBefore(now)) {
                    keyPairs.remove(entry.getKey());
                    LOG.info("Removed expired key: {}", entry.getKey());
                    return true;
                }
                return false;
            });
            return Uni.createFrom().voidItem();
        });
    }

    public Uni<String> getPublicKey(String keyId) {
        return Uni.createFrom().deferred(() -> {
            KeyPair keyPair = keyPairs.get(keyId);
            if (keyPair == null) {
                return Uni.createFrom().failure(
                        new IllegalArgumentException("Key not found: " + keyId));
            }

            String publicKey = Base64.getEncoder().encodeToString(
                    keyPair.getPublic().getEncoded());
            return Uni.createFrom().item(publicKey);
        });
    }

    private KeyPair getCurrentKeyPair() {
        if (currentKeyPair == null) {
            synchronized (this) {
                if (currentKeyPair == null) {
                    // Try to load from configuration
                    String keyId = configService.getDefaultKeyId();
                    if (keyId != null) {
                        currentKeyPair = keyPairs.get(keyId);
                    }

                    // Generate new if still null
                    if (currentKeyPair == null) {
                        String newKeyId = "default-key";
                        generateKeyPair(newKeyId).await().indefinitely();
                        currentKeyPair = keyPairs.get(newKeyId);
                    }
                }
            }
        }
        return currentKeyPair;
    }

    public Uni<Boolean> verifyTokenSignature(String token, String signature, String publicKeyBase64) {
        return Uni.createFrom().deferred(() -> {
            try {
                byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PublicKey publicKey = keyFactory.generatePublic(keySpec);

                Signature sig = Signature.getInstance("SHA256withRSA");
                sig.initVerify(publicKey);
                sig.update(token.getBytes(StandardCharsets.UTF_8));

                byte[] signatureBytes = Base64.getDecoder().decode(signature);
                return Uni.createFrom().item(sig.verify(signatureBytes));
            } catch (Exception e) {
                LOG.error("Error verifying token signature", e);
                return Uni.createFrom().item(false);
            }
        });
    }
}
