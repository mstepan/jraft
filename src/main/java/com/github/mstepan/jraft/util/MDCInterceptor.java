package com.github.mstepan.jraft.util;

import com.github.mstepan.jraft.topology.ClusterTopology;
import io.grpc.*;
import java.util.Objects;
import org.slf4j.MDC;

/** Propagates MDC custom fields to gRPC service */
public record MDCInterceptor(ClusterTopology cluster) implements ServerInterceptor {

    public MDCInterceptor {
        Objects.requireNonNull(cluster, "'cluster' can't be null");
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        // Populate all custom MDC fields
        MDC.put("nodeId", cluster.curNodeId());

        // Make normal call
        ServerCall.Listener<ReqT> listener = next.startCall(call, headers);

        // Wrap the listener to clean MDC
        return new MDCCleaner<>(listener);
    }

    private static final class MDCCleaner<ReqT>
            extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

        public MDCCleaner(ServerCall.Listener<ReqT> delegate) {
            super(delegate);
        }

        @Override
        public void onComplete() {
            MDC.clear();
            super.onComplete();
        }

        @Override
        public void onCancel() {
            MDC.clear();
            super.onCancel();
        }
    }
}
