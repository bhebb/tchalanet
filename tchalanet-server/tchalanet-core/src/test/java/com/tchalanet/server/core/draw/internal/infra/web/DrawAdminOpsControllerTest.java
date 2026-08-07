package com.tchalanet.server.core.draw.internal.infra.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import com.tchalanet.server.core.draw.api.query.GetDrawByIdQuery;
import com.tchalanet.server.core.draw.internal.infra.web.mapper.DrawAdminWebMapper;
import com.tchalanet.server.core.draw.internal.infra.web.model.AdminDrawManualResultRequest;
import com.tchalanet.server.core.drawresult.api.command.RecordManualDrawResultCommand;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DrawAdminOpsControllerTest {

  @Test
  void tenantAdminCannotTurnManualEntryIntoConfirmedResult() {
    var commandBus = mock(CommandBus.class);
    var queryBus = mock(QueryBus.class);
    var mapper = mock(DrawAdminWebMapper.class);
    var drawId = DrawId.of(UUID.randomUUID());
    var tenantId = TenantId.of(UUID.randomUUID());
    when(queryBus.ask(any(GetDrawByIdQuery.class))).thenReturn(draw(tenantId, drawId));

    var controller = new DrawAdminOpsController(commandBus, queryBus, mapper);
    var request =
        new AdminDrawManualResultRequest(
            null, "tenant proposal", "123", "4567", false, null, false);

    controller.manualResult(drawId, request, context(TchRole.TENANT_ADMIN, tenantId));

    var captor = ArgumentCaptor.forClass(RecordManualDrawResultCommand.class);
    verify(commandBus).execute(captor.capture());
    assertThat(captor.getValue().observeTrustPolicy()).isTrue();
    assertThat(captor.getValue().force()).isFalse();
  }

  @Test
  void superAdminMayConfirmManualEntry() {
    var commandBus = mock(CommandBus.class);
    var queryBus = mock(QueryBus.class);
    var mapper = mock(DrawAdminWebMapper.class);
    var drawId = DrawId.of(UUID.randomUUID());
    var tenantId = TenantId.of(UUID.randomUUID());
    when(queryBus.ask(any(GetDrawByIdQuery.class))).thenReturn(draw(tenantId, drawId));

    var controller = new DrawAdminOpsController(commandBus, queryBus, mapper);
    var request =
        new AdminDrawManualResultRequest(
            null, "platform confirmation", "123", "4567", false, null, false);

    controller.manualResult(drawId, request, context(TchRole.SUPER_ADMIN, tenantId));

    var captor = ArgumentCaptor.forClass(RecordManualDrawResultCommand.class);
    verify(commandBus).execute(captor.capture());
    assertThat(captor.getValue().observeTrustPolicy()).isFalse();
  }

  private static DrawSummary draw(TenantId tenantId, DrawId drawId) {
    return new DrawSummary(
        drawId,
        tenantId,
        LocalDate.of(2026, 8, 6),
        null,
        Instant.now(),
        null,
        null,
        null,
        null,
        null,
        DrawChannelId.of(UUID.randomUUID()),
        "TEST",
        "Test",
        LocalTime.NOON,
        "UTC",
        true,
        ResultSlotId.of(UUID.randomUUID()),
        "PA_EVE",
        "Pennsylvania",
        "UTC",
        LocalTime.NOON,
        true,
        null);
  }

  private static TchRequestContext context(TchRole role, TenantId tenantId) {
    return new TchRequestContext(
        null,
        null,
        null,
        tenantId.value(),
        null,
        Set.of(role),
        Set.of(),
        Locale.ROOT,
        "test-request",
        "127.0.0.1",
        "test",
        false,
        null,
        "active",
        ApiScope.TENANT,
        null,
        tenantId,
        null,
        null,
        null,
        null,
        null,
        Set.of(role.name()),
        Set.of("draw_result.record_manual"),
        "test-user");
  }
}
