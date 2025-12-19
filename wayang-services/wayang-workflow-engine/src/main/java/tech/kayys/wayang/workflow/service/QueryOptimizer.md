package tech.kayys.wayang.workflow.service;

import java.time.Duration;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Query Optimization & Indexing Strategy
 */
@ApplicationScoped
public class QueryOptimizer {

    /**
     * Analyze slow queries
     */
    @Scheduled(every = "1h")
    void analyzeSlowQueries() {
        slowQueryLog.getQueries(Duration.ofMinutes(60))
                .forEach(query -> {
                    if (query.executionTime() > Duration.ofSeconds(1)) {
                        log.warn("Slow query detected: {} ({}ms)",
                                query.sql(), query.executionTime().toMillis());

                        // Suggest index
                        String suggestion = indexAdvisor.suggestIndex(query);
                        if (suggestion != null) {
                            log.info("Index suggestion: {}", suggestion);
                        }
                    }
                });
    }

    /**
     * Automatic index creation (with approval)
     */
    public Uni<Void> createRecommendedIndexes() {
        return indexAdvisor.getRecommendations()
                .onItem().transformToMulti(Multi.createFrom()::iterable)
                .onItem().transformToUniAndConcatenate(recommendation -> {
                    if (recommendation.impact() > 0.3) { // > 30% improvement
                        return createIndex(recommendation)
                                .invoke(v -> log.info("Created index: {}",
                                        recommendation.indexName()));
                    }
                    return Uni.createFrom().voidItem();
                })
                .toUni().replaceWithVoid();
    }
}
