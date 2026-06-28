package com.tchalanet.server.catalog.drawchannel.internal.write;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelProvisioningApi;
import com.tchalanet.server.catalog.drawchannel.api.model.ProvisioningTenantGameRef;
import com.tchalanet.server.catalog.drawchannel.internal.cache.DrawChannelCacheNames;
import com.tchalanet.server.catalog.drawchannel.internal.persistence.DrawChannelEntity;
import com.tchalanet.server.catalog.drawchannel.internal.persistence.DrawChannelRepository;
import com.tchalanet.server.catalog.drawchannel.internal.web.model.CreateDrawChannelGameRequest;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DrawChannelProvisioningService implements DrawChannelProvisioningApi {

  private static final LocalTime DEFAULT_SALES_OPEN_TIME = LocalTime.of(5, 30);
  private static final Set<String> DEFAULT_HAITI_GAME_CODES = Set.of(
      "HT_BOLET",
      "HT_NUMERO",
      "HT_MARYAJ",
      "HT_MARYAJ_GRATUIT",
      "HT_LOTO3",
      "HT_LOTO4",
      "HT_LOTO5");

  private final DrawChannelRepository drawChannelRepository;
  private final DrawChannelGameAdminService drawChannelGameAdminService;
  private final ResultSlotCatalog resultSlotCatalog;
  private final JsonUtils jsonUtils;

  @Override
  @Transactional
  @CacheEvict(
      cacheNames = {
          DrawChannelCacheNames.BY_TENANT,
          DrawChannelCacheNames.BY_ID,
          DrawChannelCacheNames.BY_TENANT_GAME_MAP,
          DrawChannelCacheNames.BY_TENANT_BY_RESULT_SLOT_ID,
          DrawChannelCacheNames.BY_TENANT_BY_RESULT_SLOT_PROVIDER_KEY
      },
      allEntries = true)
  public void ensureDefaultHaitiLotteryChannels(
      TenantId tenantId,
      List<ProvisioningTenantGameRef> tenantGames) {
    if (tenantId == null) {
      throw new IllegalArgumentException("tenantId is required");
    }
    var enabledTenantGames = defaultHaitiTenantGames(tenantGames);
    if (enabledTenantGames.isEmpty()) {
      throw new IllegalStateException("default_haiti_lottery_games_required");
    }

    for (DefaultDrawChannel row : defaultHaitiDrawChannels()) {
      var channel = ensureChannel(tenantId, row);
      var gameRequests = enabledTenantGames.stream()
          .map(game -> new CreateDrawChannelGameRequest(
              game.tenantGameId(),
              true,
              jsonUtils.emptyObjectNode()))
          .toList();
      drawChannelGameAdminService.bulkUpsert(
          tenantId,
          DrawChannelId.of(channel.getId()),
          gameRequests);
    }
  }

  private DrawChannelEntity ensureChannel(TenantId tenantId, DefaultDrawChannel row) {
    var resultSlot = resultSlotCatalog.requireByKey(row.slotKey());
    var existing = drawChannelRepository.findFirstByTenantIdAndCodeIgnoreCaseAndDeletedAtIsNull(
        tenantId.value(),
        row.code());

    var channel = existing.orElseGet(DrawChannelEntity::new);
    channel.setTenantId(tenantId.value());
    channel.setCode(row.code());
    channel.setName(row.name());
    channel.setTimezone(resultSlot.timezone().toString());
    channel.setDrawTime(resultSlot.drawTime());
    channel.setSalesOpenTime(DEFAULT_SALES_OPEN_TIME);
    channel.setCutoffSec(row.cutoffSec());
    channel.setDaysOfWeek(row.daysOfWeek());
    channel.setActive(row.active());
    channel.setSortOrder(row.sortOrder());
    channel.setFlags(jsonUtils.emptyObjectNode());
    channel.setNotes(null);
    channel.setResultSlotId(resultSlot.id().value());
    return drawChannelRepository.saveAndFlush(channel);
  }

  private static List<ProvisioningTenantGameRef> defaultHaitiTenantGames(
      List<ProvisioningTenantGameRef> tenantGames) {
    if (tenantGames == null || tenantGames.isEmpty()) {
      return List.of();
    }
    return tenantGames.stream()
        .filter(game -> game.tenantGameId() != null)
        .filter(game -> game.gameCode() != null)
        .filter(game -> DEFAULT_HAITI_GAME_CODES.contains(game.gameCode().toUpperCase(Locale.ROOT)))
        .toList();
  }

  private static List<DefaultDrawChannel> defaultHaitiDrawChannels() {
    return List.of(
        new DefaultDrawChannel("HT_NY_MID", "Haïti • New York • Midday", "NY_MID", 300, "MON-SUN", true, 10),
        new DefaultDrawChannel("HT_NY_EVE", "Haïti • New York • Evening", "NY_EVE", 300, "MON-SUN", true, 11),
        new DefaultDrawChannel("HT_FL_MID", "Haïti • Florida • Midday", "FL_MID", 400, "MON-SUN", true, 20),
        new DefaultDrawChannel("HT_FL_EVE", "Haïti • Florida • Evening", "FL_EVE", 500, "MON-SUN", true, 21),
        new DefaultDrawChannel("HT_GA_MID", "Haïti • Georgia • Midday", "GA_MID", 300, "MON-SUN", true, 30),
        new DefaultDrawChannel("HT_GA_EVE", "Haïti • Georgia • Evening", "GA_EVE", 300, "MON-SUN", true, 31),
        new DefaultDrawChannel("HT_GA_LATE", "Haïti • Georgia • Late", "GA_LATE", 300, "MON-SUN", true, 32),
        new DefaultDrawChannel("HT_TN_MID", "Haïti • Tennessee • Midday", "TN_MID", 300, "MON-SAT", true, 40),
        new DefaultDrawChannel("HT_TN_EVE", "Haïti • Tennessee • Evening", "TN_EVE", 300, "MON-SAT", true, 41),
        new DefaultDrawChannel("HT_TX_1000", "Haïti • Texas • 10:00", "TX_1000", 300, "MON-SAT", true, 50),
        new DefaultDrawChannel("HT_TX_1227", "Haïti • Texas • 12:27", "TX_1227", 300, "MON-SAT", true, 51),
        new DefaultDrawChannel("HT_TX_1800", "Haïti • Texas • 18:00", "TX_1800", 300, "MON-SAT", true, 52),
        new DefaultDrawChannel("HT_TX_2212", "Haïti • Texas • 22:12", "TX_2212", 300, "MON-SAT", true, 53),
        new DefaultDrawChannel("HT_PA_MID", "Haïti • Pennsylvania • Midday", "PA_MID", 300, "MON-SUN", true, 60),
        new DefaultDrawChannel("HT_PA_EVE", "Haïti • Pennsylvania • Evening", "PA_EVE", 300, "MON-SUN", true, 61),
        new DefaultDrawChannel("HT_NJ_MID", "Haïti • New Jersey • Midday", "NJ_MID", 300, "MON-SUN", true, 70),
        new DefaultDrawChannel("HT_NJ_EVE", "Haïti • New Jersey • Evening", "NJ_EVE", 300, "MON-SUN", true, 71),
        new DefaultDrawChannel("HT_CA_MID", "Haïti • California • Midday", "CA_MID", 300, "MON-SUN", true, 80),
        new DefaultDrawChannel("HT_CA_EVE", "Haïti • California • Evening", "CA_EVE", 300, "MON-SUN", true, 81),
        new DefaultDrawChannel("HT_OH_MID", "Haïti • Ohio • Midday", "OH_MID", 300, "MON-SUN", true, 90),
        new DefaultDrawChannel("HT_OH_EVE", "Haïti • Ohio • Evening", "OH_EVE", 300, "MON-SUN", true, 91),
        new DefaultDrawChannel("HT_MI_MID", "Haïti • Michigan • Midday", "MI_MID", 300, "MON-SUN", true, 100),
        new DefaultDrawChannel("HT_MI_EVE", "Haïti • Michigan • Evening", "MI_EVE", 300, "MON-SUN", true, 101),
        new DefaultDrawChannel("HT_IL_MID", "Haïti • Illinois • Midday", "IL_MID", 300, "MON-SUN", true, 110),
        new DefaultDrawChannel("HT_IL_EVE", "Haïti • Illinois • Evening", "IL_EVE", 300, "MON-SUN", true, 111),
        new DefaultDrawChannel("HT_MO_MID", "Haïti • Missouri • Midday", "MO_MID", 300, "MON-SUN", true, 120),
        new DefaultDrawChannel("HT_MO_EVE", "Haïti • Missouri • Evening", "MO_EVE", 300, "MON-SUN", true, 121),
        new DefaultDrawChannel("HT_MN_EVE", "Haïti • Minnesota • Evening", "MN_EVE", 300, "MON-SUN", true, 130));
  }

  private record DefaultDrawChannel(
      String code,
      String name,
      String slotKey,
      int cutoffSec,
      String daysOfWeek,
      boolean active,
      int sortOrder) {}
}
