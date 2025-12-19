package tech.kayys.wayang.service;

import tech.kayys.wayang.schema.CollaborationEvent;

public interface CollaborationHandler {
    void onConnected(String workflowId);

    void onDisconnected(String workflowId);

    void onError(CollaborationEvent error);

    void onUserJoined(CollaborationEvent event);

    void onUserLeft(CollaborationEvent event);

    void onCursorMoved(CollaborationEvent event);

    void onNodeMoved(CollaborationEvent event);

    void onNodeLocked(CollaborationEvent event);

    void onNodeUnlocked(CollaborationEvent event);

    void onNodeUpdated(CollaborationEvent event);

    void onConnectionAdded(CollaborationEvent event);

    void onConnectionDeleted(CollaborationEvent event);

    void onSelectionChanged(CollaborationEvent event);

    void onLockFailed(CollaborationEvent event);
}
