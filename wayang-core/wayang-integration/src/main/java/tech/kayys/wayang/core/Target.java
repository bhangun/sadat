



import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Target {
    private TargetType type;
    private String id;
    
    public static Target node(String nodeId) {
        return Target.builder()
            .type(TargetType.NODE)
            .id(nodeId)
            .build();
    }
    
    public static Target workflow(String workflowId) {
        return Target.builder()
            .type(TargetType.WORKFLOW)
            .id(workflowId)
            .build();
    }
}