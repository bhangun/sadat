package tech.kayys.silat.executor.camel.modern;

import io.smallrye.mutiny.Multi;
import jakarta.websocket.Session;

record WebSocketClient(
    Session session,
    Multi.Emitter<WebSocketMessage> emitter,
    String tenantId
) {}