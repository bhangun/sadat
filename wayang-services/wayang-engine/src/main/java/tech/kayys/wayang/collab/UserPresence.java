package tech.kayys.wayang.collab;

import java.time.Instant;

/**
 * UserPresence - User presence information
 */
public class UserPresence {
    private String userId;
    private String tenantId;
    private String userName;
    private Instant joinedAt;
    private Status status;
    private boolean isTyping;
    private Instant typingAt;

    public UserPresence(String userId, String tenantId, String userName,
            Instant joinedAt, Status status) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.userName = userName;
        this.joinedAt = joinedAt;
        this.status = status;
        this.isTyping = false;
    }

    public enum Status {
        ONLINE, AWAY, OFFLINE
    }

    // Getters and setters...
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isTyping() {
        return isTyping;
    }

    public void setTyping(boolean typing) {
        isTyping = typing;
    }

    public Instant getTypingAt() {
        return typingAt;
    }

    public void setTypingAt(Instant typingAt) {
        this.typingAt = typingAt;
    }
}
