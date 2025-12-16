package tech.kayys.wayang.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * RulesDiff - Workflow rules changes
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RulesDiff {
    public List<RuleChange> allowedConnectionsChanges = new ArrayList<>();
    public List<RuleChange> disallowedConnectionsChanges = new ArrayList<>();
    public List<RuleChange> topologyRulesChanges = new ArrayList<>();
    public List<RuleChange> customRulesChanges = new ArrayList<>();
}
