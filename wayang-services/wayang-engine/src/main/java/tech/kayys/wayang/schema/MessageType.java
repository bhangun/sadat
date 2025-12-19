package tech.kayys.wayang.schema;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * MessageType - WebSocket message type (client -> server)
 */
@RegisterForReflection
public enum MessageType {
    CURSOR_MOVE,
    NODE_MOVE,
    NODE_LOCK,
    NODE_UNLOCK,
    NODE_UPDATE,
    CONNECTION_ADD,
    CONNECTION_DELETE,
    SELECTION_CHANGE
}