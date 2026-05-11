package com.tchalanet.server.core.terminal.infra.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class TerminalCacheConfig implements CacheSpecProvider {

    public static final String TERMINAL_BY_ID = "tenant.terminal.by_id";
    public static final String TERMINAL_CURRENT_BY_USER = "tenant.terminal.current_by_user";
    public static final String TERMINAL_OPERATIONAL_CONTEXT = "tenant.terminal.operational_context";
    public static final String TERMINAL_LIST = "tenant.terminal.list";

    @Override
    public List<CacheSpec> cacheSpecs() {
        return List.of(
            CacheSpec.of(TERMINAL_BY_ID, Duration.ofMinutes(20)),
            CacheSpec.of(TERMINAL_CURRENT_BY_USER, Duration.ofMinutes(5)),
            CacheSpec.of(TERMINAL_OPERATIONAL_CONTEXT, Duration.ofMinutes(5)),
            CacheSpec.of(TERMINAL_LIST, Duration.ofMinutes(5)));
    }
}
