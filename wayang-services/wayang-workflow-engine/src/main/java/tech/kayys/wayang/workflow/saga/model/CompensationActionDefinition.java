package tech.kayys.wayang.workflow.saga.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * CompensationActionDefinition - Enhanced with execution details
 */
@Embeddable
public class CompensationActionDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Action type cannot be blank")
    @Column(name = "action_type", nullable = false, length = 100)
    private String actionType;

    @NotBlank(message = "Target service cannot be blank")
    @Column(name = "target_service", nullable = false, length = 200)
    private String targetService;

    @Column(name = "compensation_method", length = 200)
    private String compensationMethod;

    @Column(name = "parameters", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> parameters = new HashMap<>();

    @Column(name = "timeout_ms")
    @Min(value = 100, message = "Timeout must be at least 100ms")
    @Max(value = 300000, message = "Timeout cannot exceed 5 minutes (300000ms)")
    private Integer timeoutMs = 30000;

    @Column(name = "is_async")
    private Boolean async = false;

    @Column(name = "fallback_action", length = 100)
    private String fallbackAction;

    @Column(name = "retry_count")
    @Min(value = 0, message = "Retry count cannot be negative")
    private Integer retryCount = 0;

    @Column(name = "priority")
    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 10, message = "Priority cannot exceed 10")
    private Integer priority = 5;

    // Default constructor
    public CompensationActionDefinition() {
    }

    // Constructor with required fields
    public CompensationActionDefinition(String actionType, String targetService) {
        this.actionType = actionType;
        this.targetService = targetService;
    }

    // Getters and Setters
    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getTargetService() {
        return targetService;
    }

    public void setTargetService(String targetService) {
        this.targetService = targetService;
    }

    public String getCompensationMethod() {
        return compensationMethod;
    }

    public void setCompensationMethod(String compensationMethod) {
        this.compensationMethod = compensationMethod;
    }

    public Map<String, Object> getParameters() {
        return parameters != null ? new HashMap<>(parameters) : new HashMap<>();
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Boolean getAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    public String getFallbackAction() {
        return fallbackAction;
    }

    public void setFallbackAction(String fallbackAction) {
        this.fallbackAction = fallbackAction;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    // Helper methods
    public void addParameter(String key, Object value) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        parameters.put(key, value);
    }

    @Override
    public String toString() {
        return "CompensationActionDefinition{" +
                "actionType='" + actionType + '\'' +
                ", targetService='" + targetService + '\'' +
                ", async=" + async +
                ", timeoutMs=" + timeoutMs +
                '}';
    }
}