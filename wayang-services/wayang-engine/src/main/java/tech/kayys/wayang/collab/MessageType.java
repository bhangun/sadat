package tech.kayys.wayang.collab;

/**
 * MessageType - Types of collaboration messages
 */
public enum MessageType {
    // Cursor tracking
    CURSOR_MOVE,

    // Workflow updates
    WORKFLOW_UPDATE,
    NODE_SELECT,

    // Typing indicators
    TYPING_START,
    TYPING_STOP,

    // Presence
    PRESENCE_UPDATE,
    PRESENCE_LIST,

    // Lock management
    LOCK_REQUEST,
    LOCK_ACQUIRED,
    LOCK_RELEASED,
    LOCK_NOTIFICATION,
    UNLOCK_REQUEST,

    // Connection
    PING,
    PONG,

    // Error
    ERROR
}
