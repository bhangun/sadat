package tech.kayys.wayang.plugin.node;


public interface NodeFactory {
    Node createNode(NodeDescriptor descriptor) throws Exception;
    boolean supports(NodeDescriptor descriptor);
}
