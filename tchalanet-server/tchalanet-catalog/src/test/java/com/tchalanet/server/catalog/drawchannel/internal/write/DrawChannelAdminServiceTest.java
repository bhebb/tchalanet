package com.tchalanet.server.catalog.drawchannel.internal.write;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawSource;
import com.tchalanet.server.catalog.drawchannel.internal.mapper.DrawChannelMapper;
import com.tchalanet.server.catalog.drawchannel.internal.persistence.DrawChannelEntity;
import com.tchalanet.server.catalog.drawchannel.internal.persistence.DrawChannelRepository;
import com.tchalanet.server.catalog.drawchannel.internal.web.model.UpdateDrawChannelRequest;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRestException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DrawChannelAdminServiceTest {

  @Test
  void updateRejectsTenantDaysOutsideResultSlotDays() {
    var repository = mock(DrawChannelRepository.class);
    var resultSlots = mock(ResultSlotCatalog.class);
    var mapper = mock(DrawChannelMapper.class);
    var service =
        new DrawChannelAdminService(repository, resultSlots, mapper, mock(JsonUtils.class));
    var channelId = DrawChannelId.of(UUID.randomUUID());
    var resultSlotId = ResultSlotId.of(UUID.randomUUID());
    var existing = new DrawChannelEntity();
    existing.setId(channelId.value());
    existing.setResultSlotId(resultSlotId.value());

    when(repository.findById(channelId.value())).thenReturn(Optional.of(existing));
    when(resultSlots.findById(resultSlotId)).thenReturn(Optional.of(resultSlot(resultSlotId)));

    var request =
        new UpdateDrawChannelRequest(
            TenantId.of(UUID.randomUUID()),
            "HT_TEST",
            "Test",
            null,
            ZoneId.of("America/Port-au-Prince"),
            LocalTime.NOON,
            LocalTime.MIDNIGHT,
            300,
            List.of(DayOfWeek.SATURDAY),
            true,
            0,
            null,
            null,
            resultSlotId,
            DrawSource.EXTERNAL);

    assertThatThrownBy(() -> service.updateFromRequest(channelId, request))
        .isInstanceOf(ProblemRestException.class)
        .satisfies(
            error ->
                assertThat(((ProblemRestException) error).getProblem().getProperties())
                    .containsEntry(
                        "code", "catalog.drawchannel.days_not_supported_by_result_slot"));
    verify(mapper, never()).updateEntityFromRequest(request, existing);
    verify(repository, never()).save(existing);
  }

  @Test
  void updateRejectsSalesOpeningAfterDrawTime() {
    var repository = mock(DrawChannelRepository.class);
    var resultSlots = mock(ResultSlotCatalog.class);
    var mapper = mock(DrawChannelMapper.class);
    var service =
        new DrawChannelAdminService(repository, resultSlots, mapper, mock(JsonUtils.class));
    var channelId = DrawChannelId.of(UUID.randomUUID());
    var resultSlotId = ResultSlotId.of(UUID.randomUUID());
    var existing = new DrawChannelEntity();
    existing.setId(channelId.value());
    existing.setResultSlotId(resultSlotId.value());

    when(repository.findById(channelId.value())).thenReturn(Optional.of(existing));
    when(resultSlots.findById(resultSlotId)).thenReturn(Optional.of(resultSlot(resultSlotId)));

    var request =
        new UpdateDrawChannelRequest(
            TenantId.of(UUID.randomUUID()),
            "HT_TEST",
            "Test",
            null,
            ZoneId.of("America/Port-au-Prince"),
            LocalTime.of(10, 0),
            LocalTime.of(10, 1),
            300,
            List.of(DayOfWeek.MONDAY),
            true,
            0,
            null,
            null,
            resultSlotId,
            DrawSource.EXTERNAL);

    assertThatThrownBy(() -> service.updateFromRequest(channelId, request))
        .isInstanceOf(ProblemRestException.class)
        .satisfies(
            error ->
                assertThat(((ProblemRestException) error).getProblem().getProperties())
                    .containsEntry("code", "catalog.drawchannel.sales_open_after_draw_time"));
    verify(mapper, never()).updateEntityFromRequest(request, existing);
    verify(repository, never()).save(existing);
  }

  @Test
  void updateRejectsSalesClosingAtDrawTime() {
    var repository = mock(DrawChannelRepository.class);
    var resultSlots = mock(ResultSlotCatalog.class);
    var mapper = mock(DrawChannelMapper.class);
    var service =
        new DrawChannelAdminService(repository, resultSlots, mapper, mock(JsonUtils.class));
    var channelId = DrawChannelId.of(UUID.randomUUID());
    var resultSlotId = ResultSlotId.of(UUID.randomUUID());
    var existing = new DrawChannelEntity();
    existing.setId(channelId.value());
    existing.setResultSlotId(resultSlotId.value());

    when(repository.findById(channelId.value())).thenReturn(Optional.of(existing));
    when(resultSlots.findById(resultSlotId)).thenReturn(Optional.of(resultSlot(resultSlotId)));

    var request =
        new UpdateDrawChannelRequest(
            TenantId.of(UUID.randomUUID()),
            "HT_TEST",
            "Test",
            null,
            ZoneId.of("America/Port-au-Prince"),
            LocalTime.of(10, 0),
            LocalTime.MIDNIGHT,
            0,
            List.of(DayOfWeek.MONDAY),
            true,
            0,
            null,
            null,
            resultSlotId,
            DrawSource.EXTERNAL);

    assertThatThrownBy(() -> service.updateFromRequest(channelId, request))
        .isInstanceOf(ProblemRestException.class)
        .satisfies(
            error ->
                assertThat(((ProblemRestException) error).getProblem().getProperties())
                    .containsEntry("code", "catalog.drawchannel.sales_close_not_before_draw_time"));
    verify(mapper, never()).updateEntityFromRequest(request, existing);
    verify(repository, never()).save(existing);
  }

  private static ResultSlotView resultSlot(ResultSlotId id) {
    return new ResultSlotView(
        id,
        "TEST_MID",
        "TEST",
        ZoneId.of("America/Port-au-Prince"),
        LocalTime.NOON,
        "MON-FRI",
        true,
        JsonUtils.emptyObject(),
        JsonUtils.emptyObject(),
        "draw_channel.test_mid.label");
  }
}
