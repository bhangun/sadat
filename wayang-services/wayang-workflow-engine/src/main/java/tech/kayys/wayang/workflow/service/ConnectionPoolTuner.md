package tech.kayys.wayang.workflow.service;

import io.quarkus.scheduler.Scheduled;
import io.agroal.api.AgroalDataSource;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Connection Pool Tuning
 */
@ApplicationScoped
public class ConnectionPoolTuner {

    @Inject
    AgroalDataSource dataSource;

    /**
     * Dynamic pool sizing based on load
     */
    @Scheduled(every = "5m")
    void tunePoolSize() {
        var metrics = dataSource.getMetrics();

        int activeConnections = (int) metrics.activeCount();
        int maxConnections = metrics.maxSize();
        double utilization = maxConnections == 0 ? 0 : (double) activeConnections / maxConnections;

        if (utilization > 0.8) {
            // Scale up
            int newSize = (int) (maxConnections * 1.2);
            dataSource.getConfiguration().connectionPoolConfiguration().setMaxSize(newSize);
            Log.info("Scaled up connection pool to " + newSize);
        } else if (utilization < 0.3) {
            // Scale down
            int newSize = (int) (maxConnections * 0.8);
            dataSource.getConfiguration().connectionPoolConfiguration().setMaxSize(Math.max(newSize, 10));
            Log.info("Scaled down connection pool to " + newSize);
        }
    }
}
