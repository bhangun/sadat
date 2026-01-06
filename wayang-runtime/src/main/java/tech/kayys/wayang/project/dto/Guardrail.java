package tech.kayys.wayang.project.dto;

import tech.kayys.wayang.guardrails.dto.GuardrailAction;

/**
 * Guardrail - Safety and compliance rules
 */
public class Guardrail {
    public String name;
    public GuardrailType type;
    public String rule;
    public GuardrailAction action;
    public int priority;
}