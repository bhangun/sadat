package tech.kayys.wayang.billing.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Workflow requirements
 */
public class Requirements {
    public List<String> requiredIntegrations;
    public List<String> requiredExecutors;
    public int minPlanTier;
    public Map<String, Integer> quotaRequirements;
    
    public Requirements() {
        this.requiredIntegrations = new ArrayList<>();
        this.requiredExecutors = new ArrayList<>();
        this.quotaRequirements = new HashMap<>();
    }
}
