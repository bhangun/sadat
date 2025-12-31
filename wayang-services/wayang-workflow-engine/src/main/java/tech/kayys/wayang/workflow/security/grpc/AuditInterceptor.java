package tech.kayys.wayang.workflow.security.grpc;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.quarkus.grpc.GlobalInterceptor;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Third interceptor in the chain.
 * Logs security audit events.
 */
@ApplicationScoped
@GlobalInterceptor
public class AuditInterceptor implements ServerInterceptor {

    private static final Logger LOG = Logger.getLogger(AuditInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String implementation = call.getMethodDescriptor().getFullMethodName();
        // LOG.info("Audit: Accessing " + implementation);
        // Uncomment/Use LOG when real audit requirements are finalized to avoid noise.

        ServerCall.Listener<ReqT> listener = next.startCall(call, headers);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(listener) {
            @Override
            public void onComplete() {
                super.onComplete();
                LOG.tracef("Audit: Completed %s", implementation);
            }

            @Override
            public void onCancel() {
                super.onCancel();
                LOG.tracef("Audit: Cancelled %s", implementation);
            }
        };
    }
}
