package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Enterprise Service Bus Integration
 */
interface ESBIntegrationService {

    /**
     * Connect to ESB
     */
    Uni<Void> connectESB(
        ESBType type, // MULESOFT, TIBCO, etc.
        ESBConfig config
    );

    /**
     * Expose workflow as ESB service
     */
    Uni<ServiceEndpoint> exposeAsService(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        ServiceConfig config
    );

    /**
     * Consume ESB service in workflow
     */
    Uni<tech.kayys.silat.core.domain.NodeDefinition> createESBNode(
        String serviceName,
        ESBOperationConfig config
    );
}