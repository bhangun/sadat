package tech.kayys.wayang.guardrails.dto;

import java.util.List;

public record SanitizedOutput(
        String content,
        List<String> modifications) {
}
