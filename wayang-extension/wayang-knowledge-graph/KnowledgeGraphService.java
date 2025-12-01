
public interface KnowledgeGraphService {
    void ingestTriple(Triple triple);
    void ingestTriples(List<Triple> triples);
    List<Triple> query(GraphQuery query);
    List<Entity> getEntities(EntityQuery query);
    List<Relation> getRelations(RelationQuery query);
    GraphResult traverse(TraversalQuery query);
    void deleteEntity(String entityId);
    void deleteTriple(String tripleId);
}