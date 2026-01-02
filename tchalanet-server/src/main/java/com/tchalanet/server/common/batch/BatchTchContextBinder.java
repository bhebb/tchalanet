package com.tchalanet.server.common.batch;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.enums.TchRole;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.stereotype.Component;

@Component
public class BatchTchContextBinder {

  public void bind(JobParameters jp) {
    // required in your job starter already
    String tenantId = jp.getString("tenant_id");
    if (tenantId == null || tenantId.isBlank()) {
      throw new IllegalArgumentException("tenant_id required (job parameter)");
    }

    String requestId = defaultStr(jp.getString("request_id"), UUID.randomUUID().toString());
    String kcUserId = jp.getString("triggered_by"); // you pass this already (Keycloak sub)
    String channelCode =
        jp.getString("channel_code"); // optional info if you want to add as property later

    // For batch, we generally set tenant_code = tenant_id (uuid string) because your filter can
    // resolve UUID from either.
    // If you want a “real” tenant_code too, pass it as job param (tenant_code) in your controller.
    String effectiveTenantCode = tenantId;

    Set<TchRole> systemRoles = new HashSet<>();
    systemRoles.add(TchRole.SUPER_ADMIN); // OR create a dedicated role SYSTEM/BATCH if you have it
    Set<String> customRoles = Set.of();

    // Locale: can be fixed or passed in params
    Locale locale = Locale.FRENCH;

    TchRequestContext ctx =
        new TchRequestContext(
            /* originalTenantCode */ effectiveTenantCode,
            /* originalTenantUuid */ UUID.fromString(tenantId),
            /* effectiveTenantCode */ effectiveTenantCode,
            /* effectiveTenantUuid */ UUID.fromString(tenantId),
            /* keycloakUserId */ kcUserId,
            /* appUserId */ null,
            /* systemRoles */ systemRoles,
            /* customRoles */ customRoles,
            /* locale */ locale,
            /* requestId */ requestId,
            /* clientIp */ "batch",
            /* userAgent */ "batch",
            /* tenantOverridden */ false,
            /* deletedVisibility */ "active");

    TchContext.set(ctx);
  }

  public void clear() {
    TchContext.clear();
  }

  private static String defaultStr(String v, String def) {
    return (v == null || v.isBlank()) ? def : v;
  }
}
