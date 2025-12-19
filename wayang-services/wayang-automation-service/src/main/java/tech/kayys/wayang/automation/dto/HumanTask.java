package tech.kayys.wayang.automation.dto;

import java.util.List;

public record HumanTask(
        String assignee,
        String description,
        List<String> actions,
        String defaultAction) {
}
