

/**
 * Plugin Isolation Manager - Manages different isolation strategies
 */
@ApplicationScoped
public class PluginIsolationManager {

    @Inject
    ClassLoaderIsolationStrategy classLoaderStrategy;

    @Inject
    WasmIsolationStrategy wasmStrategy;

    @Inject
    ContainerIsolationStrategy containerStrategy;

    /**
     * Select isolation strategy based on sandbox level
     */
    public IsolationStrategy selectStrategy(String sandboxLevel) {
        return switch (sandboxLevel.toLowerCase()) {
            case "trusted" -> classLoaderStrategy;
            case "semi-trusted" -> classLoaderStrategy; // with SecurityManager
            case "untrusted" -> wasmStrategy;
            default -> containerStrategy;
        };
    }
}
