package com.tchalanet.server.common.bus.observability;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.observability.TchTraceIds;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * CommandBus decorator that creates an Observation span for allowlisted commands.
 *
 * <p>Delegates to the target bus for all commands. Fail-open: if the
 * ObservationRegistry is noop or unavailable, the command executes normally.
 * Does not reference SimpleCommandBus directly.
 */
@Slf4j
public class ObservedCommandBus implements CommandBus {

    private final CommandBus delegate;
    private final ObservationRegistry registry;
    private final Set<String> allowlist;

    public ObservedCommandBus(
        CommandBus delegate,
        ObservationRegistry registry,
        Set<String> allowlist
    ) {
        this.delegate  = delegate;
        this.registry  = registry;
        this.allowlist = Set.copyOf(allowlist);
    }

    @Override
    public <R> R execute(Command<R> command) {
        var name = command.getClass().getSimpleName();
        if (!allowlist.contains(name)) {
            return delegate.execute(command);
        }

        var observation = Observation.createNotStarted("tch.command." + name, registry)
            .lowCardinalityKeyValue("tch.command", name)
            .lowCardinalityKeyValue("tch.request_id", nullSafe(TchTraceIds.currentRequestId()));

        observation.start();
        try {
            var result = delegate.execute(command);
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
