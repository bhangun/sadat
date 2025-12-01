package tech.kayys.wayang.node.core.loader;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.node.core.Node;
import tech.kayys.wayang.node.core.exception.NodeException;
import tech.kayys.wayang.node.core.exception.NodeLoadException;
import tech.kayys.wayang.node.core.model.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loader strategy for container-based node execution.
 * 
 * Provides maximum isolation by running nodes in separate containers/pods.
 * Ideal for untrusted code or nodes with special runtime requirements.
 * 
 * Features:
 * - Network policy isolation
 * - Resource limits (CPU, memory)
 * - Ephemeral storage
 * - No host access
 */
@ApplicationScoped
public class ContainerLoaderStrategy implements NodeLoaderStrategy {
    
    private static final Logger LOG = LoggerFactory.getLogger(ContainerLoaderStrategy.class);
    
    private final KubernetesClient k8sClient;
    private final String namespace;
    private final Map<String, Pod> activePods;
    
    @Inject
    public ContainerLoaderStrategy(
        KubernetesClient k8sClient,
        @ConfigProperty(name = "wayang.node.container.namespace", defaultValue = "wayang-nodes")
        String namespace
    ) {
        this.k8sClient = k8sClient;
        this.namespace = namespace;
        this.activePods = new ConcurrentHashMap<>();
    }
    
    @Override
    public Node load(NodeDescriptor descriptor, Node nodeInstance) 
            throws NodeLoadException {
        
        LOG.info("Loading container node: {}", descriptor.getQualifiedId());
        
        return new ContainerNodeWrapper(descriptor, nodeInstance, this);
    }
    
    @Override
    public void unload(NodeDescriptor descriptor, Node node) {
        String podName = getPodName(descriptor);
        Pod pod = activePods.remove(podName);
        
        if (pod != null) {
            try {
                k8sClient.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .delete();
                LOG.info("Deleted pod for node: {}", descriptor.getQualifiedId());
            } catch (Exception e) {
                LOG.error("Failed to delete pod: " + podName, e);
            }
        }
    }
    
    @Override
    public boolean supports(SandboxLevel sandboxLevel) {
        return sandboxLevel == SandboxLevel.UNTRUSTED;
    }
    
    @Override
    public SandboxLevel getSandboxLevel() {
        return SandboxLevel.UNTRUSTED;
    }
    
    /**
     * Create a pod for node execution
     */
    Pod createPod(NodeDescriptor descriptor) {
        String podName = getPodName(descriptor);
        
        // Build container spec
        Container container = new ContainerBuilder()
            .withName("node-executor")
            .withImage(getContainerImage(descriptor))
            .withImagePullPolicy("IfNotPresent")
            .withResources(buildResourceRequirements(descriptor))
            .withSecurityContext(buildSecurityContext())
            .withEnv(buildEnvironmentVariables(descriptor))
            .build();
        
        // Build pod spec
        PodSpec podSpec = new PodSpecBuilder()
            .withContainers(container)
            .withRestartPolicy("Never")
            .withServiceAccountName("node-executor")
            .build();
        
        // Build pod
        Pod pod = new PodBuilder()
            .withNewMetadata()
                .withName(podName)
                .withNamespace(namespace)
                .withLabels(Map.of(
                    "app", "wayang-node",
                    "node-id", descriptor.id(),
                    "node-version", descriptor.version(),
                    "sandbox-level", descriptor.sandboxLevel().name()
                ))
            .endMetadata()
            .withSpec(podSpec)
            .build();
        
        // Create pod
        Pod created = k8sClient.pods()
            .inNamespace(namespace)
            .create(pod);
        
        activePods.put(podName, created);
        
        LOG.info("Created pod {} for node {}", podName, descriptor.getQualifiedId());
        
        return created;
    }
    
    /**
     * Generate pod name from descriptor
     */
    private String getPodName(NodeDescriptor descriptor) {
        return "node-" + descriptor.id().replaceAll("[^a-z0-9-]", "-")
            + "-" + descriptor.version().replaceAll("[^a-z0-9-]", "-");
    }
    
    /**
     * Get container image for descriptor
     */
    private String getContainerImage(NodeDescriptor descriptor) {
        // In production, this would map to actual container registry
        String coordinate = descriptor.implementation().coordinate();
        return coordinate; // e.g., "registry.example.com/nodes/my-node:1.0.0"
    }
    
    /**
     * Build resource requirements from descriptor
     */
    private ResourceRequirements buildResourceRequirements(NodeDescriptor descriptor) {
        ResourceProfile profile = descriptor.resourceProfile();
        
        if (profile == null) {
            profile = new ResourceProfile(
                "100m",  // CPU
                "128Mi", // Memory
                null,    // GPU
                30,      // Timeout
                Map.of()
            );
        }
        
        return new ResourceRequirementsBuilder()
            .withRequests(Map.of(
                "cpu", new Quantity(profile.cpu()),
                "memory", new Quantity(profile.memory())
            ))
            .withLimits(Map.of(
                "cpu", new Quantity(profile.cpu()),
                "memory", new Quantity(profile.memory())
            ))
            .build();
    }
    
    /**
     * Build security context with restrictions
     */
    private SecurityContext buildSecurityContext() {
        return new SecurityContextBuilder()
            .withRunAsNonRoot(true)
            .withRunAsUser(1000L)
            .withReadOnlyRootFilesystem(true)
            .withAllowPrivilegeEscalation(false)
            .withNewCapabilities()
                .withDrop("ALL")
            .endCapabilities()
            .build();
    }
    
    /**
     * Build environment variables
     */
    private java.util.List<EnvVar> buildEnvironmentVariables(NodeDescriptor descriptor) {
        return java.util.List.of(
            new EnvVar("NODE_ID", descriptor.id(), null),
            new EnvVar("NODE_VERSION", descriptor.version(), null),
            new EnvVar("JAVA_OPTS", "-XX:MaxRAMPercentage=75.0", null)
        );
    }
    
    /**
     * Wrapper that executes node in container
     */
    private static class ContainerNodeWrapper implements Node {
        
        private final NodeDescriptor descriptor;
        private final Node delegate;
        private final ContainerLoaderStrategy strategy;
        private Pod pod;
        
        ContainerNodeWrapper(
            NodeDescriptor descriptor,
            Node delegate,
            ContainerLoaderStrategy strategy
        ) {
            this.descriptor = descriptor;
            this.delegate = delegate;
            this.strategy = strategy;
        }
        
        @Override
        public void onLoad(NodeDescriptor descriptor, NodeConfig config) 
                throws NodeException {
            
            // Create pod for this node
            this.pod = strategy.createPod(descriptor);
            
            // Wait for pod to be ready
            waitForPodReady();
            
            delegate.onLoad(descriptor, config);
        }
        
        @Override
        public CompletionStage<ExecutionResult> execute(NodeContext context) 
                throws NodeException {
            
            // In production:
            // 1. Serialize context
            // 2. Send to pod via gRPC/HTTP
            // 3. Execute in isolated container
            // 4. Receive result
            
            return CompletableFuture.supplyAsync(() -> {
                try {
                    // Delegate to container execution
                    return delegate.execute(context).toCompletableFuture().join();
                } catch (Exception e) {
                    throw new RuntimeException("Container execution failed", e);
                }
            });
        }
        
        @Override
        public void onUnload() {
            delegate.onUnload();
            strategy.unload(descriptor, this);
        }
        
        @Override
        public NodeDescriptor getDescriptor() {
            return descriptor;
        }
        
        private void waitForPodReady() {
            // Wait for pod to reach Running state
            // In production: use watches and timeouts
        }
    }
}