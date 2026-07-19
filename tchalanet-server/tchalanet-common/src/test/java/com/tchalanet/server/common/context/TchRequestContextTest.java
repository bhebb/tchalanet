package com.tchalanet.server.common.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRestException;
import java.time.ZoneId;
import java.util.Currency;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TchRequestContext")
class TchRequestContextTest {

  @Nested
  @DisplayName("compact constructor (defensive copy)")
  class DefensiveCopy {

    @Test
    @DisplayName("should defensively copy systemRoles")
    void shouldCopySystemRoles() {
      var mutableRoles = new HashSet<>(EnumSet.of(TchRole.TENANT_ADMIN));

      var ctx = ctxWith(mutableRoles, Set.of(), Set.of(), Set.of());

      mutableRoles.add(TchRole.SUPER_ADMIN);

      assertThat(ctx.systemRoles()).containsExactly(TchRole.TENANT_ADMIN);
    }

    @Test
    @DisplayName("should defensively copy customRoles")
    void shouldCopyCustomRoles() {
      var mutablePerms = new HashSet<>(Set.of("sell"));

      var ctx = ctxWith(Set.of(), mutablePerms, Set.of(), Set.of());

      mutablePerms.add("admin");

      assertThat(ctx.customRoles()).containsExactly("sell");
    }

    @Test
    @DisplayName("should default null systemRoles to empty set")
    void shouldDefaultNullSystemRoles() {
      var ctx = ctxWith(null, null, null, null);

      assertThat(ctx.systemRoles()).isEmpty();
      assertThat(ctx.customRoles()).isEmpty();
      assertThat(ctx.roleCodes()).isEmpty();
      assertThat(ctx.permissionKeys()).isEmpty();
    }
  }

  @Nested
  @DisplayName("hasPermissionClaim")
  class HasPermissionClaim {

    @Test
    @DisplayName("should match exact case")
    void shouldMatchExactCase() {
      var ctx = ctxWith(Set.of(), Set.of("ticket.read"), Set.of(), Set.of());
      assertThat(ctx.hasPermissionClaim("ticket.read")).isTrue();
    }

    @Test
    @DisplayName("should match case-insensitively")
    void shouldMatchCaseInsensitively() {
      var ctx = ctxWith(Set.of(), Set.of("Ticket.Read"), Set.of(), Set.of());
      assertThat(ctx.hasPermissionClaim("ticket.read")).isTrue();
    }

    @Test
    @DisplayName("should match uppercase stored against lowercase input")
    void shouldMatchUppercaseStoredLowercaseInput() {
      var ctx = ctxWith(Set.of(), Set.of("SELL"), Set.of(), Set.of());
      assertThat(ctx.hasPermissionClaim("sell")).isTrue();
    }

    @Test
    @DisplayName("should return false for null permission")
    void shouldReturnFalseForNull() {
      var ctx = ctxWith(Set.of(), Set.of("sell"), Set.of(), Set.of());
      assertThat(ctx.hasPermissionClaim(null)).isFalse();
    }

    @Test
    @DisplayName("should return false when no match")
    void shouldReturnFalseWhenNoMatch() {
      var ctx = ctxWith(Set.of(), Set.of("sell"), Set.of(), Set.of());
      assertThat(ctx.hasPermissionClaim("admin")).isFalse();
    }
  }

  @Nested
  @DisplayName("role helpers")
  class RoleHelpers {

    @Test
    @DisplayName("isSuperAdmin should return true when SUPER_ADMIN present")
    void shouldDetectSuperAdmin() {
      var ctx = ctxWith(EnumSet.of(TchRole.SUPER_ADMIN), Set.of(), Set.of(), Set.of());
      assertThat(ctx.isSuperAdmin()).isTrue();
      assertThat(ctx.isTenantAdmin()).isFalse();
    }

    @Test
    @DisplayName("isTenantAdmin should return true when TENANT_ADMIN present")
    void shouldDetectTenantAdmin() {
      var ctx = ctxWith(EnumSet.of(TchRole.TENANT_ADMIN), Set.of(), Set.of(), Set.of());
      assertThat(ctx.isTenantAdmin()).isTrue();
    }

    @Test
    @DisplayName("currentRole should prioritize SUPER_ADMIN > TENANT_OWNER > TENANT_ADMIN")
    void shouldPrioritizeRoles() {
      var ctx =
          ctxWith(
              EnumSet.of(TchRole.SUPER_ADMIN, TchRole.TENANT_ADMIN), Set.of(), Set.of(), Set.of());
      assertThat(ctx.currentRole()).isEqualTo(TchRole.SUPER_ADMIN);
    }

    @Test
    @DisplayName("currentRole should return null when no admin roles")
    void shouldReturnNullWhenNoAdminRoles() {
      var ctx = ctxWith(Set.of(), Set.of(), Set.of(), Set.of());
      assertThat(ctx.currentRole()).isNull();
    }
  }

  @Nested
  @DisplayName("tenant accessors")
  class TenantAccessors {

    @Test
    @DisplayName("tenantIdRequired should throw when no tenant")
    void shouldThrowWhenNoTenant() {
      var ctx =
          new TchRequestContext(
              null,
              null,
              null,
              null,
              null,
              Set.of(),
              Set.of(),
              Locale.getDefault(),
              "req",
              "127.0.0.1",
              null,
              false,
              null,
              "active",
              ApiScope.PUBLIC,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              Set.of(),
              Set.of(),
              null);
      assertThatThrownBy(ctx::tenantIdRequired)
          .isInstanceOf(ProblemRestException.class)
          .satisfies(
              error -> {
                var problem = ((ProblemRestException) error).getProblem();
                assertThat(problem.getProperties())
                    .containsEntry("code", "access.context.tenant_required")
                    .containsEntry("category", "business_rule")
                    .containsEntry("retryPolicy", "AFTER_USER_ACTION");
              });
    }

    @Test
    @DisplayName("tenantIdSafe should prefer typed tenantId over UUID fallback")
    void shouldPreferTypedTenantId() {
      var typedId = TenantId.of(UUID.randomUUID());
      var differentUuid = UUID.randomUUID();
      var ctx =
          new TchRequestContext(
              null,
              null,
              null,
              differentUuid,
              null,
              Set.of(),
              Set.of(),
              Locale.getDefault(),
              "req",
              "127.0.0.1",
              null,
              false,
              null,
              "active",
              ApiScope.TENANT,
              null,
              typedId,
              null,
              null,
              null,
              null,
              null,
              Set.of(),
              Set.of(),
              null);
      assertThat(ctx.tenantIdSafe()).isEqualTo(typedId);
    }
  }

  @Nested
  @DisplayName("deletedVisibilitySafe")
  class DeletedVisibility {

    @Test
    @DisplayName("should default to active")
    void shouldDefaultToActive() {
      var ctx = ctxWith(Set.of(), Set.of(), Set.of(), Set.of());
      assertThat(ctx.deletedVisibilitySafe()).isEqualTo("active");
    }

    @Test
    @DisplayName("should reject invalid values")
    void shouldRejectInvalidValues() {
      var ctx =
          new TchRequestContext(
              null,
              null,
              null,
              null,
              null,
              Set.of(),
              Set.of(),
              Locale.getDefault(),
              "req",
              "127.0.0.1",
              null,
              false,
              null,
              "DROP TABLE",
              ApiScope.PUBLIC,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              Set.of(),
              Set.of(),
              null);
      assertThat(ctx.deletedVisibilitySafe()).isEqualTo("active");
    }
  }

  private static TchRequestContext ctxWith(
      Set<TchRole> systemRoles,
      Set<String> customRoles,
      Set<String> roleCodes,
      Set<String> permissionKeys) {
    return new TchRequestContext(
        "test",
        UUID.randomUUID(),
        "test",
        UUID.randomUUID(),
        null,
        systemRoles,
        customRoles,
        Locale.getDefault(),
        "req-1",
        "127.0.0.1",
        null,
        false,
        null,
        "active",
        ApiScope.TENANT,
        null,
        TenantId.of(UUID.randomUUID()),
        ZoneId.of("UTC"),
        Currency.getInstance("HTG"),
        null,
        TchActorType.SYSTEM,
        null,
        roleCodes,
        permissionKeys,
        null);
  }
}
