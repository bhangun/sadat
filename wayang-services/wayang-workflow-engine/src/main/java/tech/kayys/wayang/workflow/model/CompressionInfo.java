package tech.kayys.wayang.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Compression info
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class CompressionInfo {
    private String algorithm; // gzip, lz4, zstd
    private Long originalSize;
    private Long compressedSize;
    private Double compressionRatio;
}
