package tech.kayys.wayang.workflow.version.dto;

import java.util.List;

public record TestResults(boolean allPassed, List<String> failures) {
}
