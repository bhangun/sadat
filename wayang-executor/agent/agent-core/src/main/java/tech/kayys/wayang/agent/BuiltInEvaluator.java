package tech.kayys.wayang.agent;

import java.util.Set;

/**
 * Built-in Evaluator - Evaluates agent results
 */
public record BuiltInEvaluator(
    Set<EvaluationCriteria> criteria,
    double successThreshold,
    boolean enableContinuousEvaluation
) {
    
    public static BuiltInEvaluator createDefault() {
        return new BuiltInEvaluator(
            Set.of(
                EvaluationCriteria.CORRECTNESS,
                EvaluationCriteria.COMPLETENESS,
                EvaluationCriteria.QUALITY
            ),
            0.8,
            true
        );
    }
}
