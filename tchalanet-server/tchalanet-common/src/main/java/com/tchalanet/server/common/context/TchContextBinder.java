package com.tchalanet.server.common.context;

import static com.tchalanet.server.common.context.ContextKeys.REQUEST_CONTEXT;
import static com.tchalanet.server.common.observability.TchTraceIds.MDC_REQUEST_ID;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TchContextBinder {

  public void bind(HttpServletRequest req, TchRequestContext ctx) {
    log.debug(
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

  private static final String[] MDC_KEYS = {
    "tenant_original",
    "tenant_effective",
    "tenant_overridden",
    "external_subject",
    MDC_REQUEST_ID,
    "reqId",
    "idem",
    "tenant_uuid",
    "tz",
    "ccy"
  };

  public void clear(HttpServletRequest req) {
    log.debug(
        "TchContextFilter CLEAR path={} thread={}",
        req.getRequestURI(),
        Thread.currentThread().getName());
    for (var key : MDC_KEYS) {
      MDC.remove(key);
    }
    TchContext.clear();
  }

  private void putMdc(TchRequestContext ctx) {
    MDC.put("tenant_original", valueOrDash(ctx.originalTenantCode()));
    MDC.put("tenant_effective", valueOrDash(ctx.effectiveTenantCode()));
    MDC.put("tenant_overridden", String.valueOf(ctx.tenantOverridden()));
    MDC.put("external_subject", valueOrDash(ctx.externalSubject()));
    MDC.put(MDC_REQUEST_ID, valueOrDash(ctx.requestId()));
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
