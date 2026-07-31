package com.tchalanet.server.platform.accesscontrol.internal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.context.BootstrappedActor;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.http.TchHeaders;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.accesscontrol.internal.service.AccessControlSnapshotResolver;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AccessResolutionStepPlatformTest {

  private static final UserId USER = UserId.of(UUID.randomUUID());
  private static final TenantId TENANT = TenantId.of(UUID.randomUUID());
  private static final Set<String> PLATFORM_PERMISSIONS = Set.of("platform.tenant.override");

  @Mock private AccessControlSnapshotResolver snapshotResolver;
  @Mock private EffectiveTenantResolver effectiveTenantResolver;

  private AccessResolutionStepImpl step;

  @BeforeEach
  void setUp() {
    step = new AccessResolutionStepImpl(snapshotResolver, effectiveTenantResolver);
    when(snapshotResolver.resolvePlatform(USER))
        .thenReturn(
            new AccessControlSnapshotResolver.PlatformAccess(
                true, Set.of("SUPER_ADMIN"), PLATFORM_PERMISSIONS));
  }

  @Test
  void platformSuperAdminOverrideCarriesTenantIntoRequestContext() {
    var request = new MockHttpServletRequest("POST", "/api/v1/platform/pagemodels/id/reset");
    request.addHeader(TchHeaders.X_TCH_TENANT_OVERRIDE, TENANT.value().toString());
    request.addHeader(TchHeaders.X_TCH_OVERRIDE_REASON, "manage tenant PageModel");
    when(effectiveTenantResolver.resolveForAppUser(request, USER, true, PLATFORM_PERMISSIONS))
        .thenReturn(new EffectiveTenantResolver.EffectiveTenant(TENANT, true, false));

    var resolved = step.resolveAppUser(request, appUserActor());

    assertThat(resolved.actorType()).isEqualTo(TchActorType.APP_USER);
    assertThat(resolved.effectiveTenantId()).isEqualTo(TENANT);
    assertThat(resolved.tenantOverride()).isTrue();
    assertThat(resolved.superAdmin()).isTrue();
    assertThat(resolved.roleCodes()).containsExactly("SUPER_ADMIN");
    verify(effectiveTenantResolver).resolveForAppUser(request, USER, true, PLATFORM_PERMISSIONS);
  }

  @Test
  void platformSuperAdminWithoutOverrideRemainsPlatformScoped() {
    var request = new MockHttpServletRequest("GET", "/api/v1/platform/pagemodels");

    var resolved = step.resolveAppUser(request, appUserActor());

    assertThat(resolved.effectiveTenantId()).isNull();
    assertThat(resolved.tenantOverride()).isFalse();
    assertThat(resolved.superAdmin()).isTrue();
    verifyNoInteractions(effectiveTenantResolver);
  }

  private static BootstrappedActor appUserActor() {
    return BootstrappedActor.appUser(USER, "FIREBASE", "https://issuer", "subject");
  }
}
