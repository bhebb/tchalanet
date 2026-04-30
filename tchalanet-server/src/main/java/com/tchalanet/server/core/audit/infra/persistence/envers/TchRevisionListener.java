package com.tchalanet.server.core.audit.infra.persistence.envers;

import com.tchalanet.server.common.config.SpringContextHolder;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.types.enums.AuditActorType;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.RevisionListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TchRevisionListener implements RevisionListener {

  private TchContextResolver resolver;

  public TchRevisionListener() {}

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
      rev.setActorType(ctx.userUuid() == null ? AuditActorType.SYSTEM.name() : AuditActorType.USER.name());
      rev.setApiScope(ctx.apiScope() == null ? null : ctx.apiScope().name());
      rev.setTenantOverridden(ctx.tenantOverridden());

    } catch (Exception e) {
      log.debug("Revision context resolution failed", e);
    }
  }

  private TchContextResolver resolver() {
    if (resolver == null) {
      resolver = SpringContextHolder.getBean(TchContextResolver.class);
    }
    return resolver;
  }
}
