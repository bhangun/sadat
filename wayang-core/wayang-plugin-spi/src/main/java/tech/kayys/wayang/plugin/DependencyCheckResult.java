package tech.kayys.wayang.plugin;


import java.util.ArrayList;
import java.util.List;

/**
 * Dependency Check Result
 */
public class DependencyCheckResult {
    

    private boolean satisfied = true;
    

    private List<String> missingDependencies = new ArrayList<>();
    
    public static DependencyCheckResult satisfied() {
        return DependencyCheckResult.builder().satisfied(true).build();
    }
    
    public static DependencyCheckResult unsatisfied(List<String> missing) {
        return DependencyCheckResult.builder()
            .satisfied(false)
            .missingDependencies(missing)
            .build();
    }


    public static class Builder {
        private boolean satisfied;
        private List<String> missingDependencies = new ArrayList<>();

        public Builder satisfied(boolean satisfied) {
            this.satisfied = satisfied;
            return this;
        }

        public Builder missingDependencies(List<String> missingDependencies) {
            this.missingDependencies = missingDependencies;
            return this;
        }

        public DependencyCheckResult build() {
            DependencyCheckResult result = new DependencyCheckResult();
            result.satisfied = this.satisfied;
            result.missingDependencies = this.missingDependencies;
            return result;
        }
    }

    public boolean isSatisfied() {
        return satisfied;
    }

    public void setSatisfied(boolean satisfied) {
        this.satisfied = satisfied;
    }

    public List<String> getMissingDependencies() {
        return missingDependencies;
    }

    public void setMissingDependencies(List<String> missingDependencies) {
        this.missingDependencies = missingDependencies;
    }

    public static Builder builder() {
        return new Builder();
    }
}