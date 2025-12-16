package tech.kayys.wayang.model;

import java.util.ArrayList;
import java.util.List;

public class MergeResult {
    public LogicDefinition merged;
    public UIDefinition mergedUI;
    public RuntimeConfig mergedRuntime;
    public boolean hasConflicts;
    public List<MergeConflict> conflicts = new ArrayList<>();

    public void addConflict(MergeConflict conflict) {
        this.conflicts.add(conflict);
        this.hasConflicts = true;
    }
}
