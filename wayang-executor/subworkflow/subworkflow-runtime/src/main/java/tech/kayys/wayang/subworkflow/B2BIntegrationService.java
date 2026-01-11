package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: EDI/B2B Integration
 */
interface B2BIntegrationService {

    /**
     * Parse EDI message
     */
    Uni<Map<String, Object>> parseEDI(
        String ediMessage,
        EDIStandard standard // X12, EDIFACT
    );

    /**
     * Generate EDI message
     */
    Uni<String> generateEDI(
        Map<String, Object> data,
        EDIStandard standard
    );

    /**
     * AS2/SFTP file transfer
     */
    Uni<Void> transferFile(
        byte[] fileData,
        B2BProtocol protocol,
        B2BConfig config
    );
}