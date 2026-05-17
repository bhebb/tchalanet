package com.tchalanet.server.core.sales.internal.infra.cache;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TicketId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SalesTicketCacheEvictor {

    public static final String TICKET_LIST = "core.sales.ticket.list";
    public static final String TICKET_DETAILS = "core.sales.ticket.details";
    public static final String TICKET_PRINT = "core.sales.ticket.print";
    public static final String TICKET_VERIFY = "core.sales.ticket.verify";

    private final CacheManager cacheManager;

    public void evictByDraw(DrawId drawId) {
        clear(TICKET_LIST);
        log.debug("sales.cache.evict drawId={}", drawId);
    }

    public void evictByTicket(TicketId ticketId) {
        evict(TICKET_DETAILS, ticketId.value());
        evict(TICKET_PRINT, ticketId.value());
        evict(TICKET_VERIFY, ticketId.value());
    }

    private void clear(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    private void evict(String cacheName, Object key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }
}
