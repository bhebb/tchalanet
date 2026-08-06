package com.tchalanet.server.platform.notification.internal.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationRoleCodeResolverTest {

  @Test
  void prefersTenantRoleWhenSuperAdminAlsoHasTenantContext() {
    var context =
        context(
            ApiScope.ADMIN, TenantId.of(UUID.randomUUID()), Set.of("SUPER_ADMIN", "TENANT_ADMIN"));

    assertThat(NotificationRoleCodeResolver.resolve(context)).isEqualTo("TENANT_ADMIN");
  }

  @Test
  void prefersTenantOwnerOverTenantAdmin() {
    var context =
        context(
            ApiScope.ADMIN, TenantId.of(UUID.randomUUID()), Set.of("TENANT_ADMIN", "TENANT_OWNER"));

    assertThat(NotificationRoleCodeResolver.resolve(context)).isEqualTo("TENANT_OWNER");
  }

  @Test
  void doesNotTreatPlatformAdminAsTenantAdminWithoutTenantRole() {
    var context = context(ApiScope.ADMIN, TenantId.of(UUID.randomUUID()), Set.of("SUPER_ADMIN"));

    assertThat(NotificationRoleCodeResolver.resolve(context)).isNull();
  }

  @Test
  void resolvesPlatformAdminOutsideTenantContext() {
    var context = context(ApiScope.PLATFORM, null, Set.of("SUPER_ADMIN"));

    assertThat(NotificationRoleCodeResolver.resolve(context)).isEqualTo("SUPER_ADMIN");
  }

  private static TchRequestContext context(
      ApiScope scope, TenantId tenantId, Set<String> roleCodes) {
    var systemRoles =
        roleCodes.stream()
            .map(
                roleCode -> {
                  try {
                    return TchRole.valueOf(roleCode);
                  } catch (IllegalArgumentException ignored) {
                    return null;
                  }
                })
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());
    return new TchRequestContext(
        null,
        null,
        null,
        tenantId == null ? null : tenantId.value(),
        UUID.randomUUID(),
        systemRoles,
        Set.of(),
        Locale.CANADA_FRENCH,
        "request-test",
        "127.0.0.1",
        null,
        false,
        null,
        "active",
        scope,
        null,
        tenantId,
        ZoneId.of("UTC"),
        Currency.getInstance("HTG"),
        null,
        TchActorType.APP_USER,
        null,
        roleCodes,
        Set.of(),
        null);
  }
}
