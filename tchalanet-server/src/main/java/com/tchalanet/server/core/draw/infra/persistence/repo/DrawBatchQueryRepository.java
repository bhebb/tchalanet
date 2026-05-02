package com.tchalanet.server.core.draw.infra.persistence.repo;

import com.tchalanet.server.core.draw.infra.persistence.DrawJpaEntity;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

@org.springframework.stereotype.Repository
public interface DrawBatchQueryRepository extends Repository<DrawJpaEntity, UUID> {

   // --- Copie de la méthode findSettleableDrawIds depuis SettleableDrawIdsJpaRepository ---
  @Query(
      value =
          """
          select d.id
          from draw d
          join draw_channel dc on dc.id = d.draw_channel_id
          join result_slot rs on rs.id = dc.result_slot_id
          left join draw_result dr on dr.channel_code = dc.code and dr.draw_date = (d.scheduled_at at time zone dc.timezone)::date
          where (:tenantId is null or d.tenant_id = :tenantId)
            and d.deleted_at is null and dc.deleted_at is null
            and d.locked = false
            and d.status in ('RESULTED','PENDING')
            and d.scheduled_at >= :fromTs and d.scheduled_at < :toTs
            and (:channelCode is null or dc.code = :channelCode)
            and (:provider is null or rs.provider = :provider)
            and (:source is null or d.draw_source = :source)
            and (:force = true or dr.id is null)
          order by d.scheduled_at asc
          limit coalesce(:maxDraws, 1000000000)
          """,
      nativeQuery = true)
  List<UUID> findSettleableDrawIds(
      @Param("tenantId") UUID tenantId,
      @Param("source") String source,
      @Param("provider") String provider,
      @Param("drawChannelCode") String channelCode,
      @Param("fromTs") Timestamp fromTs,
      @Param("toTs") Timestamp toTs,
      @Param("maxDraws") Long maxDraws,
      @Param("force") boolean force);
}
