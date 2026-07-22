package com.tchalanet.server.catalog.drawchannel.internal.write;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.drawchannel.api.model.ProvisioningTenantGameRef;
import com.tchalanet.server.catalog.drawchannel.internal.persistence.DrawChannelEntity;
import com.tchalanet.server.catalog.drawchannel.internal.persistence.DrawChannelRepository;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DrawChannelProvisioningServiceTest {

  @Test
  void defaultHaitiLotteryChannelsEnableOnlyNyAndFloridaForNewTenants() {
    var repository = mock(DrawChannelRepository.class);
    var channelGames = mock(DrawChannelGameAdminService.class);
    var resultSlots = mock(ResultSlotCatalog.class);
    var saved = new ArrayList<DrawChannelEntity>();

    when(repository.findFirstByTenantIdAndCodeIgnoreCaseAndDeletedAtIsNull(any(), any()))
        .thenReturn(Optional.empty());
    when(repository.saveAndFlush(any(DrawChannelEntity.class)))
        .thenAnswer(
            invocation -> {
              DrawChannelEntity channel = invocation.getArgument(0);
              channel.setId(UUID.randomUUID());
              saved.add(channel);
              return channel;
            });
    when(resultSlots.requireByKey(any()))
        .thenAnswer(invocation -> resultSlot(invocation.getArgument(0)));

    var service =
        new DrawChannelProvisioningService(
            repository, channelGames, resultSlots, mock(JsonUtils.class));
    service.ensureDefaultHaitiLotteryChannels(
        TenantId.of(UUID.randomUUID()),
        List.of(new ProvisioningTenantGameRef(TenantGameId.of(UUID.randomUUID()), "HT_BOLET")));

    assertThat(saved).isNotEmpty();
    assertThat(saved.stream().filter(DrawChannelEntity::isActive).map(DrawChannelEntity::getCode))
        .containsExactlyInAnyOrder("HT_NY_MID", "HT_NY_EVE", "HT_FL_MID", "HT_FL_EVE");
    assertThat(saved)
        .filteredOn(channel -> !channel.getCode().startsWith("HT_NY_"))
        .filteredOn(channel -> !channel.getCode().startsWith("HT_FL_"))
        .allMatch(channel -> !channel.isActive());
  }

  private static ResultSlotView resultSlot(String slotKey) {
    return new ResultSlotView(
        ResultSlotId.of(UUID.randomUUID()),
        slotKey,
        slotKey.substring(0, 2),
        ZoneId.of(slotKey.startsWith("CA_") ? "America/Los_Angeles" : "America/New_York"),
        LocalTime.NOON,
        "MON-SUN",
        true,
        JsonUtils.emptyObject(),
        JsonUtils.emptyObject(),
        "draw_channel." + slotKey.toLowerCase());
  }
}
