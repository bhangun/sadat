package tech.kayys.wayang.agent.dto;

public class SecretEntity {
    private String key;
    private String encryptedValue;

    public String getKey() {
        return key;
    }

    public String getEncryptedValue() {
        return encryptedValue;
    }
}
