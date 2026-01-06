package tech.kayys.wayang.canvas.schema;

import java.time.Instant;

/**
 * Change tracking
 */
public class ChangeRecord {
    public ChangeType type;
    public String path; // JSONPath to changed element
    public Object oldValue;
    public Object newValue;
    public String description;
    public Instant timestamp;
}
