package com.tchalanet.server.core.draw.internal.infra.cache;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.api.query.DrawSearchCriteria;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class DrawCacheKeyBuilder {

    public String today(TenantId tenantId, LocalDate date, String pageKey) {
        return "tenant:%s:date:%s:page:%s"
            .formatted(tenantId.value(), date, pageKey);
    }

    public String next(TenantId tenantId, Integer lookaheadHours, String pageKey) {
        return "tenant:%s:lookaheadHours:%s:page:%s"
            .formatted(
                tenantId.value(),
                lookaheadHours == null ? "default" : lookaheadHours,
                pageKey);
    }

    public String summary(TenantId tenantId, DrawSearchCriteria criteria, String pageKey) {
        return "tenant:%s:slot:%s:from:%s:to:%s:status:%s:page:%s"
            .formatted(
                tenantId.value(),
                criteria.resultSlotId() == null ? "all" : criteria.resultSlotId().value(),
                criteria.from(),
                criteria.to(),
                criteria.status() == null ? "all" : criteria.status(),
                pageKey);
    }

    public String latestResults(TenantId tenantId, String slotKeysKey, String pageKey) {
        return "tenant:%s:slotKeys:%s:page:%s"
            .formatted(
                tenantId.value(),
                slotKeysKey == null || slotKeysKey.isBlank() ? "all" : slotKeysKey,
                pageKey);
    }
}
