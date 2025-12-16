package tech.kayys.wayang.collab;

import java.time.Instant;

/**
 * CursorPosition - User cursor position on canvas
 */
public class CursorPosition {
    private String userId;
    private double x;
    private double y;
    private Instant timestamp;

    public CursorPosition(String userId, double x, double y) {
        this.userId = userId;
        this.x = x;
        this.y = y;
        this.timestamp = Instant.now();
    }

    // Getters and setters...
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}