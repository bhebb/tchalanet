package com.tchalanet.server.common.bus.observability;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.observability.TchTraceIds;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * QueryBus decorator that creates an Observation span for allowlisted queries.
 *
 * <p>Fail-open: noop ObservationRegistry produces no spans.
 * Does not reference SimpleQueryBus directly.
 */
@Slf4j
public class ObservedQueryBus implements QueryBus {

    private final QueryBus delegate;
    private final ObservationRegistry registry;
    private final Set<String> allowlist;

    public ObservedQueryBus(
        QueryBus delegate,
        ObservationRegistry registry,
        Set<String> allowlist
    ) {
        this.delegate  = delegate;
        this.registry  = registry;
        this.allowlist = Set.copyOf(allowlist);
    }

    @Override
    public <R> R ask(Query<R> query) {
        var name = query.getClass().getSimpleName();
        if (!allowlist.contains(name)) {
            return delegate.ask(query);
        }

        var observation = Observation.createNotStarted("tch.query." + name, registry)
            .lowCardinalityKeyValue("tch.query", name)
            .lowCardinalityKeyValue("tch.request_id", nullSafe(TchTraceIds.currentRequestId()));

        observation.start();
        try {
            var result = delegate.ask(query);
            observation.lowCardinalityKeyValue("tch.outcome", "success");
            return result;
        } catch (Exception ex) {
            observation.error(ex);
            observation.lowCardinalityKeyValue("tch.outcome", "error");
            throw ex;
        } finally {
            observation.stop();
        }
    }

    private static String nullSafe(String value) {
        return value != null ? value : "none";
    }
}
