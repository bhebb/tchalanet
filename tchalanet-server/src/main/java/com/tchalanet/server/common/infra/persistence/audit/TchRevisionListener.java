// common/infra/audit/TchRevisionListener.java
package com.tchalanet.server.common.infra.persistence.audit;

import com.tchalanet.server.common.context.RequestContextHolder;
import com.tchalanet.server.common.context.TchRequestContext;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.RevisionListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Listener Envers : remplit tenantId et userId dans revinfo à partir du TchRequestContext. */
@Component
@RequiredArgsConstructor
public class TchRevisionListener implements RevisionListener {

  private final RequestContextHolder ctxHolder;

  @Override
  public void newRevision(Object revisionEntity) {
    TchRevisionEntity rev = (TchRevisionEntity) revisionEntity;

    TchRequestContext ctx = ctxHolder.get();
    if (ctx != null) {
      if (StringUtils.hasText(ctx.tenantId())) {
        try {
          rev.setTenantId(UUID.fromString(ctx.tenantId()));
        } catch (IllegalArgumentException ignore) {
          // tenant_id non-UUID (ex. "tenant-dev-1") -> tu peux adapter ici
        }
      }
      if (StringUtils.hasText(ctx.userId())) {
        try {
          rev.setUserId(UUID.fromString(ctx.userId()));
        } catch (IllegalArgumentException ignore) {
          // pareil : adapter si besoin
        }
      }
    }
  }
}
