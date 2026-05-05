package com.tchalanet.server.core.draw.infra.cache;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.query.model.DrawSearchCriteria;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DrawCacheKeyBuilder {

    public String today(TenantId tenantId, LocalDate date, String pageKey) {
        return "tenant:%s:date:%s:page:%s"
            .formatted(tenantId.value(), date, pageKey);
    }

    public String next(TenantId tenantId, int days, String pageKey) {
        return "tenant:%s:days:%d:page:%s"
            .formatted(tenantId.value(), days, pageKey);
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
}
