package tech.kayys.wayang.hitl.dto;

import java.util.Map;

record ApproveTaskRequest(
    String comments,
    Map<String, Object> data
) {}