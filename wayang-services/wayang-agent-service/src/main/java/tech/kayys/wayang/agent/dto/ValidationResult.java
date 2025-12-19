package tech.kayys.wayang.agent.dto;

import java.util.List;

public record ValidationResult(boolean isValid, List<String> errors) {
}
