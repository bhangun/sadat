package tech.kayys.wayang.guardrails.detector;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;

@ApplicationScoped
public class BiasDetector {

    private static final Map<String, List<String>> GENDER_STEREOTYPES = Map.of(
            "female", List.of("nurturing", "emotional", "weak", "kitchen", "motherly"),
            "male", List.of("strong", "leader", "assertive", "provider", "dominant"));

    private static final List<String> RACIAL_BIAS_INDICATORS = List.of(
            "all", "always", "never", "typical", "those people", "they always");

    public Uni<BiasResult> detect(String text) {
        return Uni.createFrom().item(() -> {
            String lowerText = text.toLowerCase();

            List<String> genderBiases = detectGenderBias(lowerText);
            List<String> racialBiases = detectRacialBias(lowerText);
            boolean hasAgeBias = detectAgeBias(lowerText);

            boolean hasBias = !genderBiases.isEmpty() || !racialBiases.isEmpty() || hasAgeBias;
            double biasScore = calculateBiasScore(genderBiases, racialBiases, hasAgeBias);

            return new BiasResult(hasBias, biasScore, genderBiases, racialBiases, hasAgeBias);
        });
    }

    private List<String> detectGenderBias(String text) {
        List<String> biases = new ArrayList<>();

        GENDER_STEREOTYPES.forEach((gender, stereotypes) -> {
            stereotypes.forEach(stereotype -> {
                if (text.contains(stereotype)) {
                    biases.add(String.format("%s stereotype: %s", gender, stereotype));
                }
            });
        });

        return biases;
    }

    private List<String> detectRacialBias(String text) {
        return RACIAL_BIAS_INDICATORS.stream()
                .filter(text::contains)
                .map(indicator -> String.format("generalization: %s", indicator))
                .toList();
    }

    private boolean detectAgeBias(String text) {
        List<String> ageTerms = List.of("old", "young", "elderly", "millennial", "boomer");
        List<String> negativeTerms = List.of("stubborn", "entitled", "outdated", "immature");

        return ageTerms.stream().anyMatch(text::contains) &&
                negativeTerms.stream().anyMatch(text::contains);
    }

    private double calculateBiasScore(List<String> genderBiases, List<String> racialBiases, boolean hasAgeBias) {
        double score = 0.0;
        score += genderBiases.size() * 0.2;
        score += racialBiases.size() * 0.3;
        score += hasAgeBias ? 0.4 : 0.0;

        return Math.min(score, 1.0);
    }

    public static class BiasResult {
        private final boolean biased;
        private final double score;
        private final List<String> genderBiases;
        private final List<String> racialBiases;
        private final boolean ageBias;

        public BiasResult(boolean biased, double score, List<String> genderBiases,
                List<String> racialBiases, boolean ageBias) {
            this.biased = biased;
            this.score = score;
            this.genderBiases = genderBiases;
            this.racialBiases = racialBiases;
            this.ageBias = ageBias;
        }

        // Getters
        public boolean isBiased() {
            return biased;
        }

        public double getScore() {
            return score;
        }

        public List<String> getGenderBiases() {
            return genderBiases;
        }

        public List<String> getRacialBiases() {
            return racialBiases;
        }

        public boolean hasAgeBias() {
            return ageBias;
        }
    }
}