package tech.kayys.wayang.agent.repository;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.dto.SecretEntity;

public interface SecretRepository {
    Uni<SecretEntity> findByKey(String key, String tenantId);

    Uni<Void> save(String key, String encryptedValue, String tenantId);

    Uni<Void> delete(String key, String tenantId);
}