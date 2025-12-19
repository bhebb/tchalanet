package com.tchalanet.server.core.draw.infra.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

@org.springframework.stereotype.Repository
public interface DrawBatchQueryRepository extends Repository {

  @Query(
      value =
          """
    select
      d.id as drawId,
      d.tenant_id as tenantId,
      d.scheduled_at as scheduledAt,
      dc.code as channelCode,
      dc.external_provider as externalProvider,
      dc.external_game_key as externalGameKey,
      dc.external_channel_code as externalChannelCode,
      dc.timezone as timezone
    from draw d
    join draw_channel dc on dc.id = d.draw_channel_id
    left join draw_result dr on dr.tenant_id = d.tenant_id and dr.draw_id = d.id
    where d.status = 'CLOSED'
      and d.scheduled_at <= (:maxScheduledAt)
      and dc.active = true
      and dc.deleted_at is null
      and (dr.id is null or :force = true)
      and dc.external_provider is not null
      and dc.external_game_key is not null
      and dc.external_channel_code is not null
    order by d.scheduled_at asc
    limit :limit
  """,
      nativeQuery = true)
  List<FetchableDrawRow> findFetchable(
      @Param("maxScheduledAt") Instant maxScheduledAt,
      @Param("force") boolean force,
      @Param("limit") int limit);

  /**
   * SLOT reader: retourne les drawIds du slot tenant+channel+jour (UTC window calculée côté application).
   */
  @Query(
      value =
          """
          select d.id
          from draw d
          join draw_channel dc on dc.id = d.draw_channel_id
          left join draw_result dr on dr.tenant_id=d.tenant_id and dr.draw_id=d.id
          where d.tenant_id=:tenantId
            and dc.code=:channelCode
            and d.deleted_at is null and dc.deleted_at is null
            and dc.active=true
            and d.locked=false
            and d.status='CLOSED'
            and d.scheduled_at <= :eligibleBeforeUtc
            and d.scheduled_at >= :dayStartUtc and d.scheduled_at < :dayEndUtc
            and (dr.id is null or :force=true)
          order by d.scheduled_at asc
          limit :maxDraws
          """,
      nativeQuery = true)
  List<UUID> findClosedDrawIdsForSlot(
      @Param("tenantId") UUID tenantId,
      @Param("channelCode") String channelCode,
      @Param("dayStartUtc") Instant dayStartUtc,
      @Param("dayEndUtc") Instant dayEndUtc,
      @Param("eligibleBeforeUtc") Instant eligibleBeforeUtc,
      @Param("force") boolean force,
      @Param("maxDraws") int maxDraws);
}
