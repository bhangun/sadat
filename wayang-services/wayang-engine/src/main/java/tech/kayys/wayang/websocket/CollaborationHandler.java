package tech.kayys.wayang.websocket;

import tech.kayys.wayang.schema.CollaborationEvent;

/**
 * CollaborationHandler - Callback interface for handling WebSocket events
 */
public interface CollaborationHandler {

    void onConnected(String workflowId);

    void onDisconnected(String workflowId);

    void onUserJoined(CollaborationEvent event);

    void onUserLeft(CollaborationEvent event);

    void onCursorMoved(CollaborationEvent event);

    void onNodeMoved(CollaborationEvent event);

    void onNodeLocked(CollaborationEvent event);

    void onNodeUnlocked(CollaborationEvent event);

    void onLockFailed(CollaborationEvent event);

    void onNodeUpdated(CollaborationEvent event);

    void onConnectionAdded(CollaborationEvent event);

    void onConnectionDeleted(CollaborationEvent event);

    void onSelectionChanged(CollaborationEvent event);

    void onError(CollaborationEvent event);

    /**
     * Default implementation (no-op)
     */
    static CollaborationHandler noop() {
        return new CollaborationHandler() {
            @Override
            public void onConnected(String workflowId) {
            }

            @Override
            public void onDisconnected(String workflowId) {
            }

            @Override
            public void onUserJoined(CollaborationEvent event) {
            }

            @Override
            public void onUserLeft(CollaborationEvent event) {
            }

            @Override
            public void onCursorMoved(CollaborationEvent event) {
            }

            @Override
            public void onNodeMoved(CollaborationEvent event) {
            }

            @Override
            public void onNodeLocked(CollaborationEvent event) {
            }

            @Override
            public void onNodeUnlocked(CollaborationEvent event) {
            }

            @Override
            public void onLockFailed(CollaborationEvent event) {
            }

            @Override
            public void onNodeUpdated(CollaborationEvent event) {
            }

            @Override
            public void onConnectionAdded(CollaborationEvent event) {
            }

            @Override
            public void onConnectionDeleted(CollaborationEvent event) {
            }

            @Override
            public void onSelectionChanged(CollaborationEvent event) {
            }

            @Override
            public void onError(CollaborationEvent event) {
            }
        };
    }
}
