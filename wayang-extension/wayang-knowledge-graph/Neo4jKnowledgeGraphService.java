
@ApplicationScoped
public class Neo4jKnowledgeGraphService implements KnowledgeGraphService {
    @Inject Driver neo4jDriver;
    @Inject EntityExtractor entityExtractor;
    @Inject RelationExtractor relationExtractor;
    @Inject EmbeddingService embeddingService;
    
    @Override
    public void ingestTriple(Triple triple) {
        try (Session session = neo4jDriver.session()) {
            session.writeTransaction(tx -> {
                // Create or merge subject entity
                tx.run(
                    "MERGE (s:Entity {id: $id}) " +
                    "SET s.name = $name, s.type = $type",
                    Map.of(
                        "id", triple.getSubject().getId(),
                        "name", triple.getSubject().getName(),
                        "type", triple.getSubject().getType()
                    )
                );
                
                // Create or merge object entity
                tx.run(
                    "MERGE (o:Entity {id: $id}) " +
                    "SET o.name = $name, o.type = $type",
                    Map.of(
                        "id", triple.getObject().getId(),
                        "name", triple.getObject().getName(),
                        "type", triple.getObject().getType()
                    )
                );
                
                // Create relationship
                tx.run(
                    "MATCH (s:Entity {id: $subjectId}) " +
                    "MATCH (o:Entity {id: $objectId}) " +
                    "MERGE (s)-[r:" + triple.getPredicate() + "]->(o) " +
                    "SET r.confidence = $confidence, " +
                    "    r.source = $source, " +
                    "    r.createdAt = datetime()",
                    Map.of(
                        "subjectId", triple.getSubject().getId(),
                        "objectId", triple.getObject().getId(),
                        "confidence", triple.getConfidence(),
                        "source", triple.getSource()
                    )
                );
                
                return null;
            });
        }
    }
    
    @Override
    public List<Triple> query(GraphQuery query) {
        try (Session session = neo4jDriver.session()) {
            return session.readTransaction(tx -> {
                Result result = tx.run(query.getCypherQuery(), query.getParameters());
                
                return result.stream()
                    .map(this::recordToTriple)
                    .collect(Collectors.toList());
            });
        }
    }
    
    @Override
    public GraphResult traverse(TraversalQuery query) {
        try (Session session = neo4jDriver.session()) {
            return session.readTransaction(tx -> {
                // Build traversal query
                String cypherQuery = buildTraversalQuery(query);
                
                Result result = tx.run(cypherQuery, query.getParameters());
                
                return buildGraphResult(result);
            });
        }
    }
    
    private String buildTraversalQuery(TraversalQuery query) {
        StringBuilder cypher = new StringBuilder();
        cypher.append("MATCH path = (start:Entity {id: $startId})");
        
        if (query.getMaxDepth() > 0) {
            cypher.append("-[*1..").append(query.getMaxDepth()).append("]-");
        } else {
            cypher.append("-[*]-");
        }
        
        cypher.append("(end:Entity) ");
        
        if (query.getRelationTypes() != null && !query.getRelationTypes().isEmpty()) {
            cypher.append("WHERE type(relationships(path)[0]) IN $relationTypes ");
        }
        
        cypher.append("RETURN path");
        
        return cypher.toString();
    }
}