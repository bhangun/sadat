
@ApplicationScoped
public class VersionedSchemaRegistry implements SchemaRegistry {
    @Inject EntityManager entityManager;
    @Inject SchemaValidator schemaValidator;
    @Inject CELCompiler celCompiler;
    @Inject SchemaCache schemaCache;
    
    @Override
    @Transactional
    public void registerSchema(NodeDescriptor descriptor) {
        // Validate schema
        ValidationResult validation = schemaValidator.validate(descriptor);
        if (!validation.isValid()) {
            throw new InvalidSchemaException(validation.getErrors());
        }
        
        // Compile CEL expressions
        compileExpressions(descriptor);
        
        // Create entity
        NodeSchemaEntity entity = toEntity(descriptor);
        entityManager.persist(entity);
        
        // Invalidate cache
        schemaCache.invalidate(descriptor.getId());
    }
    
    @Override
    public Optional<NodeDescriptor> getDescriptor(String descriptorId) {
        // Check cache
        return schemaCache.get(descriptorId)
            .or(() -> loadFromDatabase(descriptorId));
    }
    
    private Optional<NodeDescriptor> loadFromDatabase(String descriptorId) {
        List<NodeSchemaEntity> entities = entityManager.createQuery(
            "SELECT s FROM NodeSchemaEntity s " +
            "WHERE s.descriptorId = :id " +
            "ORDER BY s.version DESC",
            NodeSchemaEntity.class
        )
        .setParameter("id", descriptorId)
        .setMaxResults(1)
        .getResultList();
        
        if (entities.isEmpty()) {
            return Optional.empty();
        }
        
        NodeDescriptor descriptor = toDescriptor(entities.get(0));
        schemaCache.put(descriptorId, descriptor);
        
        return Optional.of(descriptor);
    }
    
    private void compileExpressions(NodeDescriptor descriptor) {
        // Compile all CEL expressions in the descriptor
        for (PropertyDescriptor property : descriptor.getProperties()) {
            if (property.getValidationExpression() != null) {
                celCompiler.compile(property.getValidationExpression());
            }
        }
    }
}