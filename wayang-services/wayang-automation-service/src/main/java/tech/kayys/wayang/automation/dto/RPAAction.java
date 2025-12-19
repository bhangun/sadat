package tech.kayys.wayang.automation.dto;

import java.util.Map;

public record RPAAction(
                String name,
                String type, // click, type, select, wait, etc.
                Map<String, Object> parameters) {
}
