package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Legacy System Connectors
 */
interface LegacySystemConnectorService {

    /**
     * SAP connector
     */
    Uni<SAPConnection> connectSAP(SAPConfig config);

    /**
     * Mainframe connector (CICS, IMS)
     */
    Uni<MainframeConnection> connectMainframe(MainframeConfig config);

    /**
     * Oracle EBS connector
     */
    Uni<OracleEBSConnection> connectOracleEBS(OracleEBSConfig config);

    /**
     * AS400 connector
     */
    Uni<AS400Connection> connectAS400(AS400Config config);
}