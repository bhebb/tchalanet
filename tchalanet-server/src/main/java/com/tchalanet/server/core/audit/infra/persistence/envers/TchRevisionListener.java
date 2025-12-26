// common/infra/audit/TchRevisionListener.java
package com.tchalanet.server.core.audit.infra.persistence.envers;

import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.TchRequestContextHolder;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.RevisionListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Listener Envers : remplit tenantId et userId dans revinfo à partir du TchRequestContext.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TchRevisionListener implements RevisionListener {

    private final TchRequestContextHolder ctxHolder;

    @Override
    public void newRevision(Object revisionEntity) {
        TchRevisionEntity rev = (TchRevisionEntity) revisionEntity;

        TchRequestContext ctx = ctxHolder.get();
        if (ctx != null) {
            if (ctx.tenantUuid() != null) {
                try {
                    rev.setTenantId(ctx.tenantUuid());
                } catch (IllegalArgumentException ignore) {
                    log.error("Tenant uuid not found");
                }
            }
            if (ctx.userId() != null) {
                try {
                    rev.setUserId(ctx.userUuid());
                } catch (IllegalArgumentException ignore) {
                    log.error("User uuid not found");
                }
            }
        }
    }
}
