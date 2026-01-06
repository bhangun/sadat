package tech.kayys.wayang.project.dto;

public enum ErrorHandlerAction {
    RETRY,
    SKIP,
    COMPENSATE,
    ALERT,
    CUSTOM
}
