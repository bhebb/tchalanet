package com.tchalanet.server.platform.accesscontrol.api.permissionevaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.accesscontrol.api.AccessControlApi;
import com.tchalanet.server.platform.accesscontrol.api.model.request.CheckUserPermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.result.CheckUserPermissionsResult;
import java.time.ZoneId;
import java.util.Currency;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class TchPermissionEvaluatorTest {

  @Test
  void deniesWhenAuthenticationIsMissing() {
    var api = mock(AccessControlApi.class);
    var evaluator = new TchPermissionEvaluator(new StaticContextResolver(validContext()), api);

    var allowed = evaluator.hasPermission(null, null, "payout:approve");

    assertThat(allowed).isFalse();
    verify(api, never()).checkPermissions(any());
  }

  @Test
  void deniesWhenContextIsMissing() {
    var api = mock(AccessControlApi.class);
    var evaluator = new TchPermissionEvaluator(new StaticContextResolver(null), api);

    var allowed = evaluator.hasPermission(authentication(), null, "payout:approve");

    assertThat(allowed).isFalse();
    verify(api, never()).checkPermissions(any());
  }

  @Test
  void returnsAccessControlDecision() {
    var api = mock(AccessControlApi.class);
    when(api.checkPermissions(any(CheckUserPermissionsRequest.class)))
        .thenReturn(new CheckUserPermissionsResult(true, Set.of()));
    var evaluator = new TchPermissionEvaluator(new StaticContextResolver(validContext()), api);

    var allowed = evaluator.hasPermission(authentication(), null, "payout:approve");

    assertThat(allowed).isTrue();
    verify(api).checkPermissions(any(CheckUserPermissionsRequest.class));
  }

  @Test
  void deniesWhenAccessControlDenies() {
    var api = mock(AccessControlApi.class);
    when(api.checkPermissions(any(CheckUserPermissionsRequest.class)))
        .thenReturn(new CheckUserPermissionsResult(false, Set.of("payout:approve")));
    var evaluator = new TchPermissionEvaluator(new StaticContextResolver(validContext()), api);

    var allowed = evaluator.hasPermission(authentication(), null, "payout:approve");

    assertThat(allowed).isFalse();
  }

  private static UsernamePasswordAuthenticationToken authentication() {
    return new UsernamePasswordAuthenticationToken("user", "n/a", Set.of());
  }

  private static TchRequestContext validContext() {
    var tenantId = UUID.randomUUID();
    return new TchRequestContext(
        "tenant",
        tenantId,
        "tenant",
        tenantId,
        "keycloak-user",
        UUID.randomUUID(),
        EnumSet.of(TchRole.TENANT_ADMIN),
        Set.of(),
        Locale.ENGLISH,
        "request-id",
        "127.0.0.1",
        "test",
        false,
        null,
        "active",
        ApiScope.TENANT,
        null,
        TenantId.of(tenantId),
        ZoneId.of("UTC"),
        Currency.getInstance("USD"),
        null);
  }

  private static final class StaticContextResolver extends TchContextResolver {
    private final TchRequestContext context;

    private StaticContextResolver(TchRequestContext context) {
      this.context = context;
    }

    @Override
    public TchRequestContext currentOrNull() {
      return context;
    }
  }
}
