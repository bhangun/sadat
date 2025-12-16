package tech.kayys.wayang.model;

public class MergeConflict {
    public ConflictType type;
    public String id;
    public String message;
    public String branch;

    public MergeConflict(ConflictType type, String id, String message, String branch) {
        this.type = type;
        this.id = id;
        this.message = message;
        this.branch = branch;
    }
}
