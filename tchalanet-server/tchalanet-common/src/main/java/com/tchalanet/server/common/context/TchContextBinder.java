package com.tchalanet.server.common.context;

import static com.tchalanet.server.common.constant.ContextKeys.REQUEST_CONTEXT;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TchContextBinder {

  public void bind(HttpServletRequest req, TchRequestContext ctx) {
    log.info(
        "TchContextFilter SET path={} thread={} scope={} tenantCode={} tenantId={}",
        req.getRequestURI(),
        Thread.currentThread().getName(),
        ctx.apiScope(),
        ctx.effectiveTenantCode(),
        ctx.tenantIdSafe());
    req.setAttribute(REQUEST_CONTEXT, ctx);
    TchContext.set(ctx);
    putMdc(ctx);
  }

  public void clear(HttpServletRequest req) {
    log.info(
        "TchContextFilter CLEAR path={} thread={}",
        req.getRequestURI(),
        Thread.currentThread().getName());
    MDC.clear();
    TchContext.clear();
  }

  private void putMdc(TchRequestContext ctx) {
    MDC.put("tenant_original", valueOrDash(ctx.originalTenantCode()));
    MDC.put("tenant_effective", valueOrDash(ctx.effectiveTenantCode()));
    MDC.put("tenant_overridden", String.valueOf(ctx.tenantOverridden()));
    MDC.put("kc_user_id", valueOrDash(ctx.keycloakUserId()));
    MDC.put("reqId", valueOrDash(ctx.requestId()));
    MDC.put("idem", valueOrDash(ctx.idempotencyKey()));
    MDC.put("tenant_uuid", ctx.tenantIdSafe() != null ? ctx.tenantIdSafe().toString() : "-");
    MDC.put("tz", ctx.tenantZoneId() != null ? ctx.tenantZoneId().getId() : "-");
    MDC.put("ccy", ctx.tenantCurrency() != null ? ctx.tenantCurrency().getCurrencyCode() : "-");
  }

  private static String valueOrDash(String value) {
    return value != null ? value : "-";
  }
}
