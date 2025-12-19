package tech.kayys.wayang.automation.dto;

import java.util.Map;

public record TransformationSpec(
                String name,
                String type,
                String expression,
                Map<String, String> mapping) {
}