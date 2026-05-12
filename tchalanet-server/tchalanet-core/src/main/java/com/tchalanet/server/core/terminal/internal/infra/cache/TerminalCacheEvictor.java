package com.tchalanet.server.core.terminal.internal.infra.cache;

import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.application.port.out.TerminalCacheInvalidationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class TerminalCacheEvictor implements TerminalCacheInvalidationPort {

    private final CacheManager cacheManager;

    @Override
    public void evictTerminal(TerminalId terminalId) {
        evict(TerminalCacheConfig.TERMINAL_BY_ID, terminalId.value());
        evict(TerminalCacheConfig.TERMINAL_OPERATIONAL_CONTEXT, terminalId.value());
    }

    @Override
    public void evictCurrentForUser(UserId userId) {
        if (userId != null) {
            evict(TerminalCacheConfig.TERMINAL_CURRENT_BY_USER, userId.value());
        }
    }

    @Override
    public void evictTerminalAndUser(TerminalId terminalId, UserId userId) {
        evictTerminal(terminalId);
        evictCurrentForUser(userId);
    }

    @Override
    public void evictTerminalLists() {
        clear(TerminalCacheConfig.TERMINAL_LIST);
    }

    private void evict(String cacheName, Object key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    private void clear(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
