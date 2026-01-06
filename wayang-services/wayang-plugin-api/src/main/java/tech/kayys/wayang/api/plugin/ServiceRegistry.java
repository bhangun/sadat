package tech.kayys.wayang.api.plugin;

import java.util.Optional;

/**
 * Service Registry - Access to platform services
 */
public interface ServiceRegistry {
    <T> Optional<T> getService(Class<T> serviceClass);

    <T> T requireService(Class<T> serviceClass);

    boolean hasService(Class<?> serviceClass);
}
