package tech.kayys.wayang.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.plugin.executor.ExecutorRegistration;
import tech.kayys.wayang.plugin.executor.ExecutorStatus;

/**
 * Control Plane Executor Registry - Authority for executor capabilities
 */
@ApplicationScoped
public class ControlPlaneExecutorRegistry {

    private static final Logger LOG = Logger.getLogger(ControlPlaneExecutorRegistry.class);

    private final Map<String, ExecutorRegistration> executorRegistry = new ConcurrentHashMap<>();

    /**
     * Register executor with capabilities
     */
    public Uni<Void> register(ExecutorRegistration registration) {
        LOG.infof("Registering executor: %s (%s) at %s",
                registration.executorId,
                registration.protocol,
                registration.endpoint);

        executorRegistry.put(registration.executorId, registration);

        // Health check
        return performHealthCheck(registration)
                .onItem().invoke(healthy -> {
                    if (healthy) {
                        registration.status = ExecutorStatus.HEALTHY;
                        LOG.infof("Executor %s is healthy", registration.executorId);
                    } else {
                        registration.status = ExecutorStatus.UNHEALTHY;
                        LOG.warnf("Executor %s is unhealthy", registration.executorId);
                    }
                })
                .replaceWithVoid();
    }

    /**
     * Register in-process executor instance (for plugins)
     */
    public void registerInProcessExecutor(String executorId, Object instance) {
        LOG.infof("Registering in-process executor instance: %s", executorId);
        ExecutorRegistration reg = executorRegistry.get(executorId);
        if (reg != null) {
            reg.executorInstance = instance;
            reg.inProcess = true;
            reg.status = ExecutorStatus.HEALTHY;
        } else {
            // Create registration if it doesn't exist
            ExecutorRegistration newReg = new ExecutorRegistration();
            newReg.executorId = executorId;
            newReg.executorInstance = instance;
            newReg.inProcess = true;
            newReg.status = ExecutorStatus.HEALTHY;
            newReg.protocol = CommunicationProtocol.GRPC; // Stub
            executorRegistry.put(executorId, newReg);
        }
    }

    public void unregister(String executorId) {
        ExecutorRegistration removed = executorRegistry.remove(executorId);
        if (removed != null) {
            LOG.infof("Unregistered executor: %s", executorId);
        }
    }

    public ExecutorRegistration get(String executorId) {
        return executorRegistry.get(executorId);
    }

    public List<ExecutorRegistration> getAll() {
        return new ArrayList<>(executorRegistry.values());
    }

    public List<ExecutorRegistration> getByCapability(String capability) {
        return executorRegistry.values().stream()
                .filter(e -> e.capabilities.contains(capability))
                .toList();
    }

    /**
     * Resolve executor for node type
     */
    public ExecutorRegistration resolveForNode(String nodeType) {
        return executorRegistry.values().stream()
                .filter(e -> e.supportedNodes.contains(nodeType))
                .filter(e -> e.status == ExecutorStatus.HEALTHY)
                .findFirst()
                .orElse(null);
    }

    private Uni<Boolean> performHealthCheck(ExecutorRegistration registration) {
        // Implementation depends on protocol
        return Uni.createFrom().item(true);
    }
}
