package tech.kayys.wayang.sdk.dto.htil;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;



public record TaskCommentRequest(
    String text,
    String authorId
) {}
