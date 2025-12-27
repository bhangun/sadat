package tech.kayys.wayang.workflow.version.dto;

public enum ChangeType {
    NODE_REMOVED,
    NODE_ADDED,
    INPUT_CHANGED,
    OUTPUT_CHANGED,
    TYPE_CHANGED,
    TRIGGER_CHANGED,
    PROPERTY_REMOVED,
    SIGNATURE_INCOMPATIBLE
}