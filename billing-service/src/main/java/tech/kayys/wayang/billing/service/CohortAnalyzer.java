package tech.kayys.wayang.billing.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collections;
import java.util.List;
import tech.kayys.wayang.billing.model.CohortAnalysis;

@ApplicationScoped
public class CohortAnalyzer {

    public List<CohortAnalysis> analyzeCohorts(int months) {
        return Collections.emptyList();
    }
}
