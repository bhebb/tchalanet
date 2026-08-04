package com.tchalanet.server.core.analytics.internal.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.analytics.api.command.SetAnalyticsVisibilityOverrideCommand;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustScope;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsVisibilityOverrideEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsVisibilityOverrideRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SetAnalyticsVisibilityOverrideCommandHandlerTest {

  private static final UUID TENANT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID ACTOR_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final LocalDate FROM = LocalDate.of(2026, 7, 1);
  private static final LocalDate TO = LocalDate.of(2026, 7, 31);
  private static final Instant NOW = Instant.parse("2026-08-01T12:00:00Z");

  @Test
  void disablingScopePersistsReasonAndActor() {
    var repository = mock(AnalyticsVisibilityOverrideRepository.class);
    var handler = handler(repository);
    var scope = AnalyticsTrustScope.tenant(TenantId.of(TENANT_UUID), FROM, TO);

    var result =
        handler.handle(
            new SetAnalyticsVisibilityOverrideCommand(
                scope, false, "projection mismatch under investigation", ACTOR_UUID));

    verify(repository).save(any(AnalyticsVisibilityOverrideEntity.class));
    assertThat(result.visible()).isFalse();
    assertThat(result.scope()).isEqualTo(scope);
    assertThat(result.changedAt()).isEqualTo(NOW);
  }

  @Test
  void enablingScopeRemovesExactOverride() {
    var repository = mock(AnalyticsVisibilityOverrideRepository.class);
    var handler = handler(repository);
    var scope = AnalyticsTrustScope.tenant(TenantId.of(TENANT_UUID), FROM, TO);

    var result =
        handler.handle(
            new SetAnalyticsVisibilityOverrideCommand(
                scope, true, "projection has been reconciled", ACTOR_UUID));

    verify(repository).deleteExactScope(scope.type(), TENANT_UUID, null, null, FROM, TO);
    assertThat(result.visible()).isTrue();
    assertThat(result.reason()).isEqualTo("projection has been reconciled");
  }

  private static SetAnalyticsVisibilityOverrideCommandHandler handler(
      AnalyticsVisibilityOverrideRepository repository) {
    return new SetAnalyticsVisibilityOverrideCommandHandler(
        repository, Clock.fixed(NOW, ZoneOffset.UTC));
  }
}
