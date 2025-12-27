package tech.kayys.wayang.workflow.backup.service;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service for encrypting backup data
 */
@ApplicationScoped
public class EncryptionService {

    @ConfigProperty(name = "backup.encryption.algorithm", defaultValue = "AES/GCM/NoPadding")
    String encryptionAlgorithm;

    @ConfigProperty(name = "backup.encryption.key.size", defaultValue = "256")
    int keySize;

    private SecretKey secretKey;
    private Cipher cipher;

    @PostConstruct
    void initialize() {
        try {
            // In production, use a proper key management system
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(keySize);
            secretKey = keyGen.generateKey();

            cipher = Cipher.getInstance(encryptionAlgorithm);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption service", e);
        }
    }

    /**
     * Encrypt data with backup-specific salt
     */
    public byte[] encrypt(byte[] data, String backupId) {
        try {
            // Generate IV from backup ID for reproducibility
            byte[] iv = generateIVFromBackupId(backupId);

            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

            byte[] encryptedData = cipher.doFinal(data);

            // Prepend IV to encrypted data
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedData.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedData);

            return byteBuffer.array();
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt data
     */
    public byte[] decrypt(byte[] encryptedData, String backupId) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);

            byte[] iv = new byte[12]; // 96-bit IV for GCM
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

            return cipher.doFinal(cipherText);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Verify encrypted data integrity
     */
    public boolean verifyIntegrity(byte[] encryptedData, String backupId) {
        try {
            // Try to decrypt to verify integrity
            decrypt(encryptedData, backupId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] generateIVFromBackupId(String backupId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(backupId.getBytes());
            return Arrays.copyOf(hash, 12); // 96-bit IV
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate IV", e);
        }
    }
}
