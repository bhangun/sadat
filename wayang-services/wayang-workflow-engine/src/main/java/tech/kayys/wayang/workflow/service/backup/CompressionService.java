package tech.kayys.wayang.workflow.service.backup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service for compressing backup data
 */
@ApplicationScoped
public class CompressionService {

    @ConfigProperty(name = "backup.compression.algorithm", defaultValue = "GZIP")
    String compressionAlgorithm;

    @ConfigProperty(name = "backup.compression.level", defaultValue = "6")
    int compressionLevel;

    /**
     * Compress data using configured algorithm
     */
    public byte[] compress(byte[] data) {
        if (data.length == 0) {
            return data;
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                GZIPOutputStream gzipOS = new GZIPOutputStream(bos) {
                    {
                        def.setLevel(compressionLevel);
                    }
                }) {

            gzipOS.write(data);
            gzipOS.finish();
            return bos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Compression failed", e);
        }
    }

    /**
     * Decompress data
     */
    public byte[] decompress(byte[] compressedData) {
        if (compressedData.length == 0) {
            return compressedData;
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
                GZIPInputStream gzipIS = new GZIPInputStream(bis);
                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIS.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }

            return bos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Decompression failed", e);
        }
    }

    /**
     * Get compression ratio
     */
    public double getCompressionRatio(byte[] original, byte[] compressed) {
        if (original.length == 0)
            return 1.0;
        return (double) compressed.length / original.length;
    }
}