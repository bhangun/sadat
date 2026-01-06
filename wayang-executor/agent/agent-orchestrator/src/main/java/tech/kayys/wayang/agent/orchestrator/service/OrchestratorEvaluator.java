package tech.kayys.wayang.agent.orchestrator.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.EvaluationCriteria;
import tech.kayys.wayang.agent.dto.AgentExecutionPlan;
import tech.kayys.wayang.agent.dto.AgentExecutionResult;

/**
 * ============================================================================
 * ORCHESTRATOR EVALUATOR
 * ============================================================================
 * 
 * Built-in evaluator for assessing orchestration quality
 */
@ApplicationScoped
public class OrchestratorEvaluator {
    
    private static final Logger LOG = LoggerFactory.getLogger(OrchestratorEvaluator.class);
    
    /**
     * Evaluate orchestration results
     */
    public Uni<EvaluationResult> evaluate(
            AgentExecutionPlan plan,
            Map<String, AgentExecutionResult> stepResults,
            Map<String, Object> context) {
        
        LOG.debug("Evaluating orchestration results");
        
        return Uni.createFrom().item(() -> {
            // Evaluate each criterion
            Map<EvaluationCriteria, Double> scores = new EnumMap<>(EvaluationCriteria.class);
            
            scores.put(EvaluationCriteria.CORRECTNESS, 
                evaluateCorrectness(stepResults));
            scores.put(EvaluationCriteria.COMPLETENESS, 
                evaluateCompleteness(plan, stepResults));
            scores.put(EvaluationCriteria.QUALITY, 
                evaluateQuality(stepResults));
            scores.put(EvaluationCriteria.EFFICIENCY, 
                evaluateEfficiency(stepResults));
            
            // Calculate overall score (weighted average)
            double overallScore = calculateOverallScore(scores);
            
            // Generate recommendations
            List<String> recommendations = generateRecommendations(scores);
            
            return new EvaluationResult(
                overallScore,
                scores,
                recommendations,
                Instant.now()
            );
        });
    }
    
    private double evaluateCorrectness(Map<String, AgentExecutionResult> results) {
        long successful = results.values().stream()
            .filter(AgentExecutionResult::isSuccess)
            .count();
        return (double) successful / results.size();
    }
    
    private double evaluateCompleteness(
            AgentExecutionPlan plan,
            Map<String, AgentExecutionResult> results) {
        return (double) results.size() / plan.steps().size();
    }
    
    private double evaluateQuality(Map<String, AgentExecutionResult> results) {
        return results.values().stream()
            .mapToDouble(r -> r.metrics().successScore())
            .average()
            .orElse(0.0);
    }
    
    private double evaluateEfficiency(Map<String, AgentExecutionResult> results) {
        // Lower execution time = higher efficiency
        long avgTime = results.values().stream()
            .mapToLong(r -> r.metrics().executionTimeMs())
            .sum() / results.size();
        
        // Normalize (assuming 30s is baseline)
        return Math.max(0, 1.0 - (avgTime / 30000.0));
    }
    
    private double calculateOverallScore(Map<EvaluationCriteria, Double> scores) {
        // Weighted average
        double correctnessWeight = 0.4;
        double completenessWeight = 0.3;
        double qualityWeight = 0.2;
        double efficiencyWeight = 0.1;
        
        return scores.get(EvaluationCriteria.CORRECTNESS) * correctnessWeight +
               scores.get(EvaluationCriteria.COMPLETENESS) * completenessWeight +
               scores.get(EvaluationCriteria.QUALITY) * qualityWeight +
               scores.get(EvaluationCriteria.EFFICIENCY) * efficiencyWeight;
    }
    
    private List<String> generateRecommendations(Map<EvaluationCriteria, Double> scores) {
        List<String> recommendations = new ArrayList<>();
        
        if (scores.get(EvaluationCriteria.CORRECTNESS) < 0.8) {
            recommendations.add("Consider using more specialized agents for better accuracy");
        }
        if (scores.get(EvaluationCriteria.EFFICIENCY) < 0.7) {
            recommendations.add("Optimize task distribution for better efficiency");
        }
        
        return recommendations;
    }
    
    public record EvaluationResult(
        double overallScore,
        Map<EvaluationCriteria, Double> criteriaScores,
        List<String> recommendations,
        Instant evaluatedAt
    ) {}
}