package tech.kayys.wayang.node.repository;

import java.util.List;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.node.domain.NodeMetadata;

/**
 * Node metadata repository.
 */
@ApplicationScoped
class NodeMetadataRepository implements PanacheRepositoryBase<NodeMetadata, Long> {

    public Uni<NodeMetadata> findByNodeType(String nodeType) {
        return find("nodeType", nodeType).firstResult();
    }

    public Uni<List<NodeMetadata>> findByCapability(String capability) {
        return find("? member of capabilities", capability).list();
    }

    public Uni<Void> deleteByNodeType(String nodeType) {
        return delete("nodeType", nodeType).replaceWithVoid();
    }
}
