package tech.kayys.wayang.guardrails.dto;

public enum GuardrailAction {
    BLOCK,
    WARN,
    SANITIZE,
    LOG,
    ALLOW,
    REDACT,
    FILTER
}