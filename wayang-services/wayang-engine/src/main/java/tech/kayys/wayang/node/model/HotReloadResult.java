package tech.kayys.wayang.node.model;

import tech.kayys.wayang.node.domain.NodeMetadata;

/**
 * Hot reload result.
 */
@lombok.Data
@lombok.Builder
class HotReloadResult {
    private String status;
    private String message;
    private NodeMetadata newMetadata;

    public static HotReloadResult success(NodeMetadata metadata) {
        return HotReloadResult.builder()
                .status("SUCCESS")
                .newMetadata(metadata)
                .build();
    }

    public static HotReloadResult failed(String message) {
        return HotReloadResult.builder()
                .status("FAILED")
                .message(message)
                .build();
    }
}
