package tech.kayys.wayang.plugin.executor;

import java.net.URI;
import java.util.Set;

import tech.kayys.wayang.plugin.CommunicationProtocol;

/**
 * Executor Descriptor in contract
 */
public class ExecutorDescriptor {
    public String executorId;
    public Set<String> capabilities;
    public URI endpoint;
    public CommunicationProtocol protocol;
}