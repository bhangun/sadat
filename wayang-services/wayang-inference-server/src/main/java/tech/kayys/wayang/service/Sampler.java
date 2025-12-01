package tech.kayys.wayang.service;


public class Sampler {
    private final SamplingConfig config;
    private final Random rng;
    private final List<Integer> lastTokens;
    
    public Sampler(SamplingConfig config, int seed) {
        this.config = config;
        this.rng = seed >= 0 ? new Random(seed) : new Random();
        this.lastTokens = new ArrayList<>(config.repeatLastN());
    }
    
    public int sample(MemorySegment logits, int vocabSize) {
        float[] logitsArray = new float[vocabSize];
        for (int i = 0; i < vocabSize; i++) {
            logitsArray[i] = logits.getAtIndex(ValueLayout.JAVA_FLOAT, i);
        }
        
        // Apply repeat penalty
        if (config.repeatPenalty() != 1.0f && !lastTokens.isEmpty()) {
            for (int token : lastTokens) {
                if (logitsArray[token] > 0) {
                    logitsArray[token] /= config.repeatPenalty();
                } else {
                    logitsArray[token] *= config.repeatPenalty();
                }
            }
        }
        
        // Apply frequency and presence penalties
        if (config.frequencyPenalty() != 0.0f || config.presencePenalty() != 0.0f) {
            Map<Integer, Integer> counts = new HashMap<>();
            for (int token : lastTokens) {
                counts.merge(token, 1, Integer::sum);
            }
            for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
                int token = entry.getKey();
                float penalty = config.frequencyPenalty() * entry.getValue() + config.presencePenalty();
                logitsArray[token] -= penalty;
            }
        }
        
        // Top-K filtering
        if (config.topK() > 0 && config.topK() < vocabSize) {
            float[] sorted = logitsArray.clone();
            Arrays.sort(sorted);
            float threshold = sorted[vocabSize - config.topK()];
            for (int i = 0; i < vocabSize; i++) {
                if (logitsArray[i] < threshold) {
                    logitsArray[i] = Float.NEGATIVE_INFINITY;
                }
            }
        }
        
        // Temperature
        if (config.temperature() > 0 && config.temperature() != 1.0f) {
            for (int i = 0; i < vocabSize; i++) {
                logitsArray[i] /= config.temperature();
            }
        }
        
        // Softmax
        float maxLogit = Float.NEGATIVE_INFINITY;
        for (float logit : logitsArray) {
            if (logit > maxLogit && !Float.isInfinite(logit)) {
                maxLogit = logit;
            }
        }
        
        float sumExp = 0.0f;
        float[] probs = new float[vocabSize];
        for (int i = 0; i < vocabSize; i++) {
            if (Float.isInfinite(logitsArray[i])) {
                probs[i] = 0.0f;
            } else {
                probs[i] = (float) Math.exp(logitsArray[i] - maxLogit);
                sumExp += probs[i];
            }
        }
        
        for (int i = 0; i < vocabSize; i++) {
            probs[i] /= sumExp;
        }
        
        // Min-P filtering
        if (config.minP() > 0.0f) {
            float maxProb = 0.0f;
            for (float p : probs) {
                if (p > maxProb) maxProb = p;
            }
            float threshold = config.minP() * maxProb;
            sumExp = 0.0f;
            for (int i = 0; i < vocabSize; i++) {
                if (probs[i] < threshold) {
                    probs[i] = 0.0f;
                } else {
                    sumExp += probs[i];
                }
            }
            if (sumExp > 0) {
                for (int i = 0; i < vocabSize; i++) {
                    probs[i] /= sumExp;
                }
            }
        }
        
        // Top-P (nucleus) sampling
        if (config.topP() < 1.0f) {
            List<TokenProb> sorted = new ArrayList<>();
            for (int i = 0; i < vocabSize; i++) {
                if (probs[i] > 0) {
                    sorted.add(new TokenProb(i, probs[i]));
                }
            }
            sorted.sort((a, b) -> Float.compare(b.prob, a.prob));
            
            float cumProb = 0.0f;
            int cutoff = 0;
            for (int i = 0; i < sorted.size(); i++) {
                cumProb += sorted.get(i).prob;
                if (cumProb >= config.topP()) {
                    cutoff = i + 1;
                    break;
                }
            }
            
            Arrays.fill(probs, 0.0f);
            sumExp = 0.0f;
            for (int i = 0; i < cutoff; i++) {
                TokenProb tp = sorted.get(i);
                probs[tp.token] = tp.prob;
                sumExp += tp.prob;
            }
            
            if (sumExp > 0) {
                for (int i = 0; i < vocabSize; i++) {
                    probs[i] /= sumExp;
                }
            }
        }
        
        // Sample
        float random = rng.nextFloat();
        float cumulative = 0.0f;
        for (int i = 0; i < vocabSize; i++) {
            cumulative += probs[i];
            if (random < cumulative) {
                accept(i);
                return i;
            }
        }
        
        for (int i = vocabSize - 1; i >= 0; i--) {
            if (probs[i] > 0) {
                accept(i);
                return i;
            }
        }
        
        accept(0);
        return 0;
    }
    
    public void accept(int token) {
        lastTokens.add(token);
        if (lastTokens.size() > config.repeatLastN()) {
            lastTokens.remove(0);
        }
    }
    
    public void reset() {
        lastTokens.clear();
    }
    
    private record TokenProb(int token, float prob) {}
}
