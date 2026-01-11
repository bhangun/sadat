package tech.kayys.silat.executor.camel.modern;

import io.smallrye.mutiny.Multi;
import jakarta.websocket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

@ClientEndpoint
class WebSocketMessageEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketMessageEndpoint.class);

    private final Multi.Emitter<WebSocketMessage> emitter;
    private final String tenantId;

    public WebSocketMessageEndpoint(
            Multi.Emitter<WebSocketMessage> emitter,
            String tenantId) {
        this.emitter = emitter;
        this.tenantId = tenantId;
    }

    @OnOpen
    public void onOpen(Session session) {
        LOG.info("WebSocket connected: {}", session.getId());
        emitter.emit(new WebSocketMessage(
            "CONNECTED",
            session.getId(),
            null,
            tenantId,
            Instant.now()
        ));
    }

    @OnMessage
    public void onMessage(String message) {
        emitter.emit(new WebSocketMessage(
            "MESSAGE",
            null,
            message,
            tenantId,
            Instant.now()
        ));
    }

    @OnClose
    public void onClose(CloseReason reason) {
        LOG.info("WebSocket closed: {}", reason);
        emitter.emit(new WebSocketMessage(
            "CLOSED",
            null,
            reason.getReasonPhrase(),
            tenantId,
            Instant.now()
        ));
        emitter.complete();
    }

    @OnError
    public void onError(Throwable error) {
        LOG.error("WebSocket error", error);
        emitter.fail(error);
    }
}