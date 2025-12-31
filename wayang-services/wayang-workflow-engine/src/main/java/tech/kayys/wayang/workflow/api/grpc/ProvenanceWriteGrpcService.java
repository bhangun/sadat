package tech.kayys.wayang.workflow.api.grpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.security.annotations.ExecutionSecured;
import com.google.protobuf.Empty;

// Implementation note: This service corresponds to runtime event recording.
// Currently acts as a placeholder or needs to implement a Write-specific gRPC interface.
// Assuming for now it handles high-volume runtime events secure by Execution Tokens.

@GrpcService
@ExecutionSecured
public class ProvenanceWriteGrpcService {
    // TODO: Implement specific Write methods defined in proto
    // If proto doesn't exist, this remains a placeholder for the architecture.
}
