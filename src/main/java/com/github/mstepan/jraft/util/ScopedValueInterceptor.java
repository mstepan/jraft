package com.github.mstepan.jraft.util;

import com.github.mstepan.jraft.topology.ClusterTopology;
import io.grpc.*;

/**
 * Interceptor that propagates ClusterTopology object from ScopeValue to a gRPC contexts, so that
 * ClusterTopology object can be used inside server implementation.
 *
 * @param cluster - cluster topology.
 */
public record ScopedValueInterceptor(ClusterTopology cluster) implements ServerInterceptor {

    public static final Context.Key<ClusterTopology> CLUSTER_TOPOLOGY =
            Context.key("cluster-topology");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        Context ctx = Context.current().withValue(CLUSTER_TOPOLOGY, cluster);
        return Contexts.interceptCall(ctx, call, headers, next);
    }
}
