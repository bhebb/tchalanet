package com.tchalanet.server.core.draw.internal.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.api.command.OpenDueDrawsCommand;
import com.tchalanet.server.core.draw.api.query.ExistingDrawKey;
import com.tchalanet.server.core.draw.api.query.NewDrawRow;
import com.tchalanet.server.core.draw.api.query.OpenableDrawRow;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.internal.application.port.out.ResultSlotCalendarReaderPort;
import com.tchalanet.server.core.draw.internal.application.query.projection.DueToCloseRow;
import com.tchalanet.server.core.draw.internal.domain.model.Draw;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OpenDueDrawsCommandHandlerTest {

  @Test
  void providerClosedScheduledDrawIsCanceledInsteadOfOpened() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var openDrawId = DrawId.of(UUID.randomUUID());
    var canceledDrawId = DrawId.of(UUID.randomUUID());
    var openSlotId = ResultSlotId.of(UUID.randomUUID());
    var closedSlotId = ResultSlotId.of(UUID.randomUUID());
    var drawDate = LocalDate.of(2026, 1, 2);
    var now = Instant.parse("2026-01-02T14:00:00Z");
    var port =
        new FakeDrawLifecyclePort(
            List.of(
                new OpenableDrawRow(
                    tenantId, openDrawId, openSlotId, drawDate, false, now, now.plusSeconds(3600)),
                new OpenableDrawRow(
                    tenantId,
                    canceledDrawId,
                    closedSlotId,
                    drawDate,
                    false,
                    now,
                    now.plusSeconds(3600))));
    var calendar = new FakeResultSlotCalendarReaderPort(closedSlotId, drawDate);
    var handler = new OpenDueDrawsCommandHandler(port, calendar);

    var result = handler.handle(new OpenDueDrawsCommand(now, 100, 24, 0, false));

    assertThat(result.opened()).isEqualTo(1);
    assertThat(result.canceledProviderClosed()).isEqualTo(1);
    assertThat(port.opened).containsExactly(openDrawId);
    assertThat(port.canceled).containsExactly(canceledDrawId);
    assertThat(port.reasonCode).isEqualTo("PROVIDER_CLOSED");
  }

  private static final class FakeResultSlotCalendarReaderPort
      implements ResultSlotCalendarReaderPort {
    private final ResultSlotId closedSlotId;
    private final LocalDate closedDate;

    private FakeResultSlotCalendarReaderPort(ResultSlotId closedSlotId, LocalDate closedDate) {
      this.closedSlotId = closedSlotId;
      this.closedDate = closedDate;
    }

    @Override
    public Set<LocalDate> findUnavailableDates(
        ResultSlotId resultSlotId, LocalDate fromInclusive, LocalDate toInclusive) {
      return closedSlotId.equals(resultSlotId) && closedDate.equals(fromInclusive)
          ? Set.of(closedDate)
          : Set.of();
    }
  }

  private static final class FakeDrawLifecyclePort implements DrawLifecyclePort {
    private final List<OpenableDrawRow> rows;
    private final List<DrawId> opened = new ArrayList<>();
    private final List<DrawId> canceled = new ArrayList<>();
    private String reasonCode;

    private FakeDrawLifecyclePort(List<OpenableDrawRow> rows) {
      this.rows = rows;
    }

    @Override
    public List<OpenableDrawRow> findOpenable(
        Instant now, int limit, int openHorizonHours, int openLagHours) {
      return rows;
    }

    @Override
    public List<OpenableDrawRow> findOpenableForSalesOpenTime(
        Instant now, LocalDate drawDate, LocalTime defaultSalesOpenTime, int limit) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int bulkOpen(List<DrawId> drawIds, Instant now) {
      opened.addAll(drawIds);
      return drawIds.size();
    }

    @Override
    public int bulkCancelScheduled(
        List<DrawId> drawIds, String reasonCode, String reasonLabel, Instant now) {
      canceled.addAll(drawIds);
      this.reasonCode = reasonCode;
      return drawIds.size();
    }

    @Override
    public List<DueToCloseRow> findDueToClose(Instant now, int limit) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int bulkClose(List<DrawId> drawIds, Instant now) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int bulkInsert(List<NewDrawRow> rows) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<ExistingDrawKey> findExistingKeys(TenantId tenantId, LocalDate from, LocalDate to) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Draw save(Draw draw) {
      throw new UnsupportedOperationException();
    }
  }
}
