package com.tchalanet.server.uslottery.infra;

import com.tchalanet.server.draw.domain.model.Draw;
import com.tchalanet.server.draw.domain.model.DrawSource;
import com.tchalanet.server.draw.domain.model.DrawStatus;
import com.tchalanet.server.draw.domain.ports.DrawRepository;
import com.tchalanet.server.draw.infra.persistence.DrawChannelJpaEntity;
import com.tchalanet.server.draw.infra.persistence.DrawChannelJpaRepository;
import com.tchalanet.server.uslottery.domain.dto.LatestDrawDto;
import com.tchalanet.server.uslottery.domain.ports.out.UsLotteryDrawSyncPort;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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

  private final DrawRepository drawRepository;
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

    // 3. Construire le payload résultat minimal
    Map<String, Object> payload =
        Map.of(
            "channelCode", dto.externalChannelCode(),
            "scheduledAt", dto.scheduledAt().toString(),
            "resultPayloadJson", dto.resultPayloadJson());

    // Pour l'instant, on utilise externalKey comme gameCode nominal
    String gameCode = dto.externalChannelCode();

    // Créer un draw de base en SCHEDULED puis appliquer le résultat via la logique métier
    Draw base =
        new Draw(
            UUID.randomUUID(),
            tenantId,
            channel.getId(),
            gameCode,
            scheduledAt,
            channel.getCutoffSec(),
            DrawStatus.SCHEDULED,
            DrawSource.US_LOTTERY,
            dto.resultPayloadJson(),
            Boolean.FALSE,
            Boolean.FALSE);

    Draw resulted = base.applyResult(payload, DrawSource.US_LOTTERY, null);

    // 5. Insérer si inexistant, sinon laisser la logique existante gérer les updates
    boolean inserted = drawRepository.saveIfNotExists(resulted);
    if (inserted) {
      log.info(
          "uslottery-sync: inserted RESULTED draw for tenant={} channel={} date{}",
          tenantId,
          dto.externalChannelCode(),
          dto.scheduledAt());
    } else {
      log.debug(
          "uslottery-sync: draw already exists for tenant={} channel={} date={}",
          tenantId,
          dto.externalChannelCode(),
          dto.scheduledAt());
    }
  }
}
