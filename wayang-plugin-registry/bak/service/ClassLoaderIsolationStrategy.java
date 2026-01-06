
/**
 * ClassLoader Isolation Strategy
 */
@ApplicationScoped
public class ClassLoaderIsolationStrategy implements IsolationStrategy {

    private static final Logger LOG = Logger.getLogger(ClassLoaderIsolationStrategy.class);

    @Override
    public Uni<Node> loadPlugin(PluginDescriptor descriptor, byte[] artifactData) {
        return Uni.createFrom().item(() -> {
            try {
                // Create isolated classloader
                URL[] urls = new URL[] { 
                    createJarUrl(artifactData) 
                };
                
                URLClassLoader classLoader = AccessController.doPrivileged(
                    (PrivilegedAction<URLClassLoader>) () -> 
                        new PluginClassLoader(urls, getClass().getClassLoader())
                );

                // Load main class
                String entrypoint = descriptor.getImplementation().getEntrypoint();
                Class<?> pluginClass = classLoader.loadClass(entrypoint);

                // Instantiate node
                Node instance = (Node) pluginClass.getDeclaredConstructor().newInstance();
                instance.onLoad(descriptor, new NodeConfig());

                LOG.infof("Loaded plugin class: %s", entrypoint);
                return instance;

            } catch (Exception e) {
                throw new PluginLoadException("Failed to load plugin via classloader", e);
            }
        });
    }

    @Override
    public Uni<Boolean> unloadPlugin(Node instance) {
        return Uni.createFrom().item(() -> {
            try {
                instance.onUnload();
                return true;
            } catch (Exception e) {
                LOG.error("Error unloading plugin", e);
                return false;
            }
        });
    }

    @Override
    public String getName() {
        return "ClassLoader";
    }

    private URL createJarUrl(byte[] jarBytes) throws IOException {
        // Write to temp file and create URL
        Path tempFile = Files.createTempFile("plugin-", ".jar");
        Files.write(tempFile, jarBytes);
        return tempFile.toUri().toURL();
    }
}