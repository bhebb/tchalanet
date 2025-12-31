package com.tchalanet.server.core.audit.infra.persistence.envers;

import com.tchalanet.server.common.context.TchContextResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.RevisionListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TchRevisionListener implements RevisionListener {

  private final TchContextResolver resolver;

  @Override
  public void newRevision(Object revisionEntity) {
    var rev = (TchRevisionEntity) revisionEntity;

    try {
      var ctx = resolver.currentOrNull();
      if (ctx == null) return;

      if (ctx.tenantUuid() != null) {
        rev.setTenantId(ctx.tenantUuid());
      }
      if (ctx.userUuid() != null) {
        rev.setUserId(ctx.userUuid());
      }

    } catch (Exception e) {
      log.debug("Revision context resolution failed", e);
    }
  }
}
