package tech.kayys.wayang.agent.repository;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.dto.ApiKeyEntity;

public interface ApiKeyRepository {
    Uni<ApiKeyEntity> findByKey(String apiKey, String tenantId);

    Uni<Void> updateLastUsed(String id);
}
