package tech.kayys.wayang.api.node;

public interface NodeFactory {
    Node createNode(NodeDescriptor descriptor) throws Exception;

    boolean supports(NodeDescriptor descriptor);

    /**
     * Validate if this factory can create the node
     */
    void validate(NodeDescriptor descriptor);
}
