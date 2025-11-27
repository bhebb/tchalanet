package com.tchalanet.server.common.infra.persistence.audit;

import static com.tchalanet.server.common.domain.AppConstants.REQUEST_CONTEXT;

import com.tchalanet.server.common.infra.persistence.BaseTenantEntity;
import jakarta.persistence.PrePersist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Slf4j
public class TenantEntityListener {

  @PrePersist
  public void prePersist(Object entity) {
    if (entity instanceof BaseTenantEntity tenantEntity) {
      try {
        if (tenantEntity.getTenantId() == null) {
          RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
          if (attrs != null) {
            Object ctx = attrs.getAttribute(REQUEST_CONTEXT, RequestAttributes.SCOPE_REQUEST);
            if (ctx instanceof com.tchalanet.server.common.context.TchRequestContext tch) {
              var tenant = tch.tenantUuid();
              if (tenant != null) {
                tenantEntity.setTenantId(tenant);
              }
            }
          }
        }
      } catch (Exception ex) {
        log.warn("Failed to set tenant_id from request context", ex);
      }
    }
  }
}
