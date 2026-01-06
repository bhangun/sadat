package tech.kayys.wayang.plugin.runtime.repository;

import java.util.List;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.plugin.runtime.PluginEntity;
import tech.kayys.wayang.plugin.runtime.PluginStatus;

/**
 * Plugin Repository - Reactive Panache Repository
 */
@ApplicationScoped
public class PluginRepository implements PanacheRepository<PluginEntity> {

    public Uni<PluginEntity> findByIdAndVersion(String pluginId, String version) {
        return find("pluginId = ?1 and version = ?2", pluginId, version)
            .firstResult();
    }

    public Uni<List<PluginEntity>> findWithQuery(PluginQuery query) {
        StringBuilder hql = new StringBuilder("FROM PluginEntity WHERE 1=1");
        
        if (query.getStatus() != null) {
            hql.append(" AND status = :status");
        }
        if (query.getTenantId() != null) {
            hql.append(" AND tenantId = :tenantId");
        }
        if (query.getCapability() != null) {
            hql.append(" AND jsonb_exists(descriptor->'capabilities', :capability)");
        }

        hql.append(" ORDER BY createdAt DESC");

        PanacheQuery<PluginEntity> panacheQuery = find(hql.toString());
        
        if (query.getStatus() != null) {
            panacheQuery.withHint("status", PluginStatus.valueOf(query.getStatus()));
        }
        if (query.getTenantId() != null) {
            panacheQuery.withHint("tenantId", query.getTenantId());
        }
        if (query.getCapability() != null) {
            panacheQuery.withHint("capability", query.getCapability());
        }

        return panacheQuery
            .page(query.getPage(), query.getSize())
            .list();
    }

    public Uni<List<PluginEntity>> findByStatus(PluginStatus status) {
        return find("status", status).list();
    }

    public Uni<List<PluginEntity>> findByCapability(String capability) {
        return find("jsonb_exists(descriptor->'capabilities', ?1)", capability).list();
    }
}