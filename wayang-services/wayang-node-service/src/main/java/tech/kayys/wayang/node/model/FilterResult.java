package tech.kayys.wayang.workflow.model;

import java.util.List;

@lombok.Data
@lombok.Builder
public class FilterResult {
    private final boolean harmful;
    private final List<String> categories;
}
