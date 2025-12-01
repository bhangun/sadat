@Singleton
class BucketRegistry {
    
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    @CacheResult(cacheName = "rate-limit-buckets")
    public Bucket getBucket(String tenantId) {
        return buckets.computeIfAbsent(tenantId, k -> {
            Bandwidth limit = Bandwidth.builder()
                .capacity(1000)
                .refillGreedy(1000, Duration.ofHours(1))
                .build();
            
            return Bucket.builder()
                .addLimit(limit)
                .build();
        });
    }
}