package tech.kayys.wayang.canvas.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeValidation {
    public boolean isValid = true;
    public List<ValidationIssue> issues = new ArrayList<>();
    public Map<String, Object> metadata = new HashMap<>();
}