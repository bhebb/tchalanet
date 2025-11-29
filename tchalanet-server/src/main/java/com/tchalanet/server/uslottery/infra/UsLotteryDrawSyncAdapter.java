package com.tchalanet.server.uslottery.infra;

import com.tchalanet.server.draw.application.port.out.DrawWriterPort;
import com.tchalanet.server.draw.domain.model.Draw;
import com.tchalanet.server.draw.domain.model.DrawChannelId;
import com.tchalanet.server.draw.domain.model.DrawResult;
import com.tchalanet.server.draw.domain.model.DrawSource;
import com.tchalanet.server.draw.domain.model.DrawStatus;
import com.tchalanet.server.draw.infra.persistence.DrawChannelJpaEntity;
import com.tchalanet.server.draw.infra.persistence.DrawChannelJpaRepository;
import com.tchalanet.server.uslottery.domain.dto.LatestDrawDto;
import com.tchalanet.server.uslottery.domain.ports.out.UsLotteryDrawSyncPort;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Adapter qui mappe les LatestDrawDto (NY/FL) vers la table draw en utilisant DrawRepository. */
@Component
@RequiredArgsConstructor
@Slf4j
public class UsLotteryDrawSyncAdapter implements UsLotteryDrawSyncPort {

  private final DrawWriterPort drawWriterPort;
  private final DrawChannelJpaRepository drawChannelJpaRepository;

  /** Tenant par défaut pour les tirages US Lottery (peut être surchargé par propriété). */
  @Value("${tch.us-lottery.default-tenant-id:00000000-0000-0000-0000-000000000002}")
  private UUID defaultTenantId; // par défaut: demo

  public void syncLatestDraws(List<LatestDrawDto> latestDraws) {
    if (latestDraws == null || latestDraws.isEmpty()) {
      log.info("uslottery-sync: no draws to sync");
      return;
    }
    log.info("uslottery-sync: syncing {} US lottery draws", latestDraws.size());

    for (LatestDrawDto dto : latestDraws) {
      try {
        syncSingle(dto);
      } catch (Exception e) {
        log.warn(
            "uslottery-sync: failed to sync draw channel={} date={}: {}",
            dto.externalChannelCode(),
            dto.scheduledAt(),
            e.toString());
      }
    }
  }

  private void syncSingle(LatestDrawDto dto) {
    UUID tenantId = defaultTenantId;

    Optional<DrawChannelJpaEntity> maybeChannel =
        drawChannelJpaRepository.findByTenantIdAndCode(tenantId, dto.externalChannelCode());

    if (maybeChannel.isEmpty()) {
      log.debug(
          "uslottery-sync: no active draw_channel for tenant={} and code={} (provider={})",
          tenantId,
          dto.externalChannelCode(),
          "US_LOTTERY");
      return;
    }

    DrawChannelJpaEntity channel = maybeChannel.get();

    // DTO déjà fournit scheduledAt en Instant
    Instant scheduledAt = dto.scheduledAt();

    // 3. Construire le DrawResult à partir du payload
    // TODO: Parser resultPayloadJson pour extraire numbersMain, etc.
    // Pour l'instant, dummy
    List<String> numbersMain = List.of("1", "2", "3", "4", "5"); // dummy
    List<String> numbersExtra = List.of("6"); // dummy
    DrawResult result =
        new DrawResult(
            DrawSource.US_LOTTERY,
            numbersMain,
            numbersExtra,
            scheduledAt,
            dto.resultPayloadJson(),
            false,
            null);

    // 4. Créer le Draw avec le résultat
    UUID drawId = UUID.randomUUID();
    ZonedDateTime scheduledZdt = scheduledAt.atZone(java.time.ZoneId.systemDefault());
    ZonedDateTime cutoffZdt = scheduledZdt.minusSeconds(channel.getCutoffSec());

    Draw draw =
        new Draw(
            drawId,
            tenantId,
            new DrawChannelId(channel.getId()),
            scheduledZdt,
            cutoffZdt,
            DrawStatus.RESULTED,
            result);

    // 5. Sauvegarder
    drawWriterPort.save(draw);
    log.info(
        "uslottery-sync: synced RESULTED draw for tenant={} channel={} date={}",
        tenantId,
        dto.externalChannelCode(),
        dto.scheduledAt());
  }
}
