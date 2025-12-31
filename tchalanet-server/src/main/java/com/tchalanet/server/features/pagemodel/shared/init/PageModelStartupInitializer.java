package com.tchalanet.server.features.pagemodel.shared.init;

import com.tchalanet.server.common.constant.CommonConstants;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchRequestContext;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PageModelStartupInitializer implements ApplicationRunner {

  private final PageModelBootstrapService bootstrapService;

  @Override
  public void run(ApplicationArguments args) {

    // Provide a minimal TchRequestContext for startup seeding to satisfy RLS and auditing
    UUID defaultTenantUuid = CommonConstants.DEFAULT_TENANT_UUID;

    var ctx =
        new TchRequestContext(
            "tchalanet", // originalTenantCode
            defaultTenantUuid, // originalTenantUuid  (IMPORTANT)
            "tchalanet", // effectiveTenantCode
            defaultTenantUuid, // effectiveTenantUuid (IMPORTANT)
            null, // keycloakUserId
            null, // appUserId
            Set.of(), // systemRoles
            Set.of(), // customRoles
            Locale.FRENCH,
            "startup", // requestId
            "127.0.0.1",
            null,
            false,
            "active");

    TchContext.set(ctx);
    try {
      bootstrapService.seedDefaultsForDefaultTenant();
    } finally {
      TchContext.clear();
    }
  }
}
