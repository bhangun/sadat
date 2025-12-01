
@Value
@Builder
public class GenerationRequest {
    UUID workflowId;
    GenerationTarget target;
    GenerationOptions options;
    boolean publish;
    PublishConfig publishConfig;
}
