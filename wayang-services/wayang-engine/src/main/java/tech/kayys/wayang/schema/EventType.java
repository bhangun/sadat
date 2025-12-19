package tech.kayys.wayang.schema;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * EventType - WebSocket event type (server -> client)
 */
@RegisterForReflection
public enum EventType {
    USER_JOINED,
    USER_LEFT,
    CURSOR_MOVED,
    NODE_MOVED,
    NODE_LOCKED,
    NODE_UNLOCKED,
    LOCK_FAILED,
    NODE_UPDATED,
    CONNECTION_ADDED,
    CONNECTION_DELETED,
    SELECTION_CHANGED,
    VALIDATION_COMPLETED,
    ERROR
}
