package tech.kayys.wayang.hitl.dto;

import jakarta.validation.constraints.NotBlank;

record AddCommentRequest(
    @NotBlank String comment
) {}