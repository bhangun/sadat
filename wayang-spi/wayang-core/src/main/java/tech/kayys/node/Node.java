public interface Node {
    NodeDescriptor getDescriptor();
    ExecutionResult execute(NodeContext context) throws NodeExecutionException;
    void onLoad(NodeConfig config) throws NodeException;
    void onUnload();
    void validate(NodeContext context) throws ValidationException;
}