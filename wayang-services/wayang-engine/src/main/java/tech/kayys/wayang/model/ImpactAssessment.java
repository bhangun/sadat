package tech.kayys.wayang.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * ImpactAssessment - Assessment of change impact
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImpactAssessment {

    public ChangeImpact impact = ChangeImpact.UNKNOWN;
    public int affectedNodes = 0;
    public int affectedConnections = 0;
    public boolean affectsDownstream = false;
    public boolean affectsUpstream = false;
    public boolean requiresRetesting = false;
    public List<String> affectedNodeIds = new ArrayList<>();
    public List<String> warnings = new ArrayList<>();
}
