package tech.kayys.wayang.hitl.dto;

import jakarta.validation.constraints.NotBlank;

record DelegateTaskRequest(
    @NotBlank String toUserId,
    @NotBlank String reason
) {}