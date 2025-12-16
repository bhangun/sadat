


/**
 * Selection policy implementation with scoring algorithm
 */
@ApplicationScoped
public class SelectionPolicy {
    
    private final RuntimeMetricsCache metricsCache;
    private final HardwareDetector hardwareDetector;
    
    /**
     * Rank available runners based on multiple criteria
     */
    public List<RunnerCandidate> rankRunners(
        ModelManifest manifest,
        RequestContext context,
        List<String> configuredRunners
    ) {
        List<RunnerCandidate> candidates = new ArrayList<>();
        
        // Get current hardware availability
        HardwareCapabilities hw = hardwareDetector.detect();
        
        for (String runnerName : configuredRunners) {
            RunnerMetadata runnerMeta = getRunnerMetadata(runnerName);
            
            // Filter by format compatibility
            if (!hasCompatibleFormat(manifest, runnerMeta)) {
                continue;
            }
            
            // Filter by device availability
            if (!isDeviceAvailable(runnerMeta, hw, context)) {
                continue;
            }
            
            // Calculate score
            int score = calculateScore(
                manifest, 
                runnerMeta, 
                context, 
                hw
            );
            
            candidates.add(new RunnerCandidate(
                runnerName, 
                score, 
                runnerMeta
            ));
        }
        
        // Sort by score descending
        candidates.sort(Comparator.comparing(
            RunnerCandidate::score
        ).reversed());
        
        return candidates;
    }
    
    /**
     * Multi-factor scoring algorithm
     */
    private int calculateScore(
        ModelManifest manifest,
        RunnerMetadata runner,
        RequestContext context,
        HardwareCapabilities hw
    ) {
        int score = 0;
        
        // Device preference match (highest weight)
        if (context.preferredDevice().isPresent() &&
            runner.supportedDevices().contains(
                context.preferredDevice().get()
            )) {
            score += 50;
        }
        
        // Format native support
        if (runner.supportedFormats().contains(
            manifest.artifacts().keySet().iterator().next()
        )) {
            score += 30;
        }
        
        // Historical performance (P95 latency)
        Optional<Duration> p95 = metricsCache.getP95Latency(
            runner.name(), 
            manifest.modelId()
        );
        if (p95.isPresent() && 
            p95.get().compareTo(context.timeout()) < 0) {
            score += 25;
        }
        
        // Resource availability
        if (hasAvailableResources(manifest, runner, hw)) {
            score += 20;
        }
        
        // Health status
        if (metricsCache.isHealthy(runner.name())) {
            score += 15;
        }
        
        // Cost optimization (favor CPU over GPU if performance OK)
        if (context.costSensitive() && 
            runner.supportedDevices().contains(DeviceType.CPU)) {
            score += 10;
        }
        
        // Current load (avoid overloaded runners)
        double currentLoad = metricsCache.getCurrentLoad(runner.name());
        if (currentLoad < 0.7) {
            score += 10;
        } else if (currentLoad > 0.9) {
            score -= 20;
        }
        
        return score;
    }
}