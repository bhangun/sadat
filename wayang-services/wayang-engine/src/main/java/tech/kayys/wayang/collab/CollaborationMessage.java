package tech.kayys.wayang.collab;

import java.time.Instant;
import java.util.List;

/**
 * CollaborationMessage - WebSocket message structure
 */
public class CollaborationMessage {
    private MessageType type;
    private String senderId;
    private Instant timestamp;

    // Cursor tracking
    private CursorPosition cursor;

    // Workflow operations
    private WorkflowOperation operation;

    // Presence
    private UserPresence presence;
    private PresenceEvent presenceEvent;
    private List<UserPresence> presenceList;

    // Lock management
    private String lockId;

    // Error
    private String error;

    // Node selection
    private String selectedNodeId;

    // Getters and setters...
    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public CursorPosition getCursor() {
        return cursor;
    }

    public void setCursor(CursorPosition cursor) {
        this.cursor = cursor;
    }

    public WorkflowOperation getOperation() {
        return operation;
    }

    public void setOperation(WorkflowOperation operation) {
        this.operation = operation;
    }

    public UserPresence getPresence() {
        return presence;
    }

    public void setPresence(UserPresence presence) {
        this.presence = presence;
    }

    public PresenceEvent getPresenceEvent() {
        return presenceEvent;
    }

    public void setPresenceEvent(PresenceEvent presenceEvent) {
        this.presenceEvent = presenceEvent;
    }

    public List<UserPresence> getPresenceList() {
        return presenceList;
    }

    public void setPresenceList(List<UserPresence> presenceList) {
        this.presenceList = presenceList;
    }

    public String getLockId() {
        return lockId;
    }

    public void setLockId(String lockId) {
        this.lockId = lockId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getSelectedNodeId() {
        return selectedNodeId;
    }

    public void setSelectedNodeId(String selectedNodeId) {
        this.selectedNodeId = selectedNodeId;
    }
}