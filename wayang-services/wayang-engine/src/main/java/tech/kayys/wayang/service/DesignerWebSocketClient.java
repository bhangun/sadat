package tech.kayys.wayang.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DesignerWebSocketClient {

    private static final Logger LOG = Logger.getLogger(DesignerWebSocketClient.class);

    public Uni<Void> connect(String workflowId, String userId, String tenantId, CollaborationHandler handler) {
        LOG.infof("Mock connecting to collaboration WS for %s", workflowId);
        return Uni.createFrom().voidItem();
    }
}
