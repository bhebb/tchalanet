package com.tchalanet.server.platform.audit.internal.listener;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.spring.SpringBeans;
import com.tchalanet.server.platform.audit.api.model.AuditActorType;
import com.tchalanet.server.platform.audit.internal.persistence.TchRevisionEntity;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.RevisionListener;

@Slf4j
public class TchRevisionListener implements RevisionListener {

    private TchContextResolver resolver;

    public TchRevisionListener() {
        // Required by Hibernate Envers.
    }

    TchRevisionListener(TchContextResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void newRevision(Object revisionEntity) {
        var rev = (TchRevisionEntity) revisionEntity;

        try {
            var ctx = resolver().currentOrNull();
            if (ctx == null) {
                rev.setActorType(AuditActorType.SYSTEM.name());
                return;
            }

            if (ctx.tenantUuid() != null) {
                rev.setTenantId(ctx.tenantUuid());
            }
            if (ctx.userUuid() != null) {
                rev.setUserId(ctx.userUuid());
            }

            rev.setRequestId(ctx.requestId());
            rev.setActorType(ctx.userUuid() == null
                ? AuditActorType.SYSTEM.name()
                : AuditActorType.USER.name());
            rev.setApiScope(ctx.apiScope() == null ? null : ctx.apiScope().name());
            rev.setTenantOverridden(ctx.tenantOverridden());

        } catch (Exception e) {
            log.debug("Revision context resolution failed", e);
        }
    }

    private TchContextResolver resolver() {
        if (resolver == null) {
            resolver = SpringBeans.getBean(TchContextResolver.class);
        }
        return resolver;
    }
}
