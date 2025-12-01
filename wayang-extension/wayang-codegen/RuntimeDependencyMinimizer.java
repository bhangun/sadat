
// Dependency Minimizer
@ApplicationScoped
public class RuntimeDependencyMinimizer {
    public MinimizedDependencies minimize(WorkflowModel model) {
        Set<Dependency> required = new HashSet<>();
        
        // Always include core
        required.add(new Dependency("io.quarkus", "quarkus-arc", null));
        
        // Analyze nodes for required dependencies
        for (NodeModel node : model.getNodes()) {
            required.addAll(analyzeDependencies(node));
        }
        
        return MinimizedDependencies.builder()
            .dependencies(new ArrayList<>(required))
            .build();
    }
    
    private Set<Dependency> analyzeDependencies(NodeModel node) {
        Set<Dependency> deps = new HashSet<>();
        
        // Check capabilities
        if (node.getDescriptor().getCapabilities().contains(Capability.LLM_ACCESS)) {
            deps.add(new Dependency("io.quarkiverse.langchain4j", "quarkus-langchain4j", null));
        }
        
        if (node.getDescriptor().getCapabilities().contains(Capability.DB_ACCESS)) {
            deps.add(new Dependency("io.quarkus", "quarkus-hibernate-orm-panache", null));
            deps.add(new Dependency("io.quarkus", "quarkus-jdbc-postgresql", null));
        }
        
        return deps;
    }
}