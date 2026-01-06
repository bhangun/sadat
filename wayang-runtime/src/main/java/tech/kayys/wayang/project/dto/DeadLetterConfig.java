package tech.kayys.wayang.project.dto;

import java.util.Map;

public class DeadLetterConfig {
    public boolean enabled;
    public String destination;
    public Map<String, Object> config;
}
