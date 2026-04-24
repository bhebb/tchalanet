package com.tchalanet.server.common.context;

import com.tchalanet.server.common.types.id.TenantId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TchContextResolver {

  public TchRequestContext currentOrNull() {
    return TchContext.currentOrNull();
  }

  public TchRequestContext currentOrThrow() {
    var ctx = currentOrNull();
    if (ctx == null) {
      throw new IllegalStateException("No TchRequestContext bound to current thread");
    }
    return ctx;
  }

  public static TenantId mapToTenantId(TchRequestContext ctx) {
    if (ctx == null) return null;
    return ctx.tenantIdSafe();
  }
}
