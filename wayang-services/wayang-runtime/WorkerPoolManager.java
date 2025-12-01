@ApplicationScoped
public class WorkerPoolManager {
    private final Map<String, WorkerPool> pools = new ConcurrentHashMap<>();
    
    @Inject PoolAutoscaler autoscaler;
    @Inject HealthChecker healthChecker;
    
    @PostConstruct
    void initialize() {
        // Initialize default pools
        pools.put("trusted-jvm", new JvmWorkerPool("trusted-jvm", 10, 50));
        pools.put("untrusted-wasm", new WasmWorkerPool("untrusted-wasm", 5, 20));
        pools.put("gpu", new GpuWorkerPool("gpu", 2, 10));
        pools.put("container", new ContainerWorkerPool("container", 5, 30));
    }
    
    public WorkerPool selectPool(ExecuteNodeTask task) {
        NodeDescriptor descriptor = task.getNodeDescriptor();
        
        // GPU requirement
        if (descriptor.getResourceProfile().isRequiresGpu()) {
            return pools.get("gpu");
        }
        
        // Sandbox level
        switch (descriptor.getSandboxLevel()) {
            case TRUSTED:
                return pools.get("trusted-jvm");
            case SEMI_TRUSTED:
                return pools.get("trusted-jvm"); // With isolation
            case UNTRUSTED:
                return pools.get("untrusted-wasm");
            default:
                return pools.get("container");
        }
    }
}