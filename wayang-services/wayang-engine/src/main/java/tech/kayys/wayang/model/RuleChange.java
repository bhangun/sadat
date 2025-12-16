package tech.kayys.wayang.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

import org.eclipse.microprofile.graphql.Ignore;

/**
 * RuleChange - Individual rule change
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuleChange {
    public ChangeType type;
    public String ruleId;
    public String ruleType;
    @Ignore
    public Object oldValue;
    @Ignore
    public Object newValue;
}
