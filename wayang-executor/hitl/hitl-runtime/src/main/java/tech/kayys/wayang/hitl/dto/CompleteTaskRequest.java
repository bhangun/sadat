package tech.kayys.wayang.hitl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

record CompleteTaskRequest(
    String outcome,
    String comments,
    @NotNull Map<String, Object> data
) {}