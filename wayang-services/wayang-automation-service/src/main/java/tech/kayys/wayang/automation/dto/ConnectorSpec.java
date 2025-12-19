package tech.kayys.wayang.automation.dto;

import java.util.Map;

public record ConnectorSpec(
                String connectorId,
                String name,
                Map<String, Object> configuration) {
}
