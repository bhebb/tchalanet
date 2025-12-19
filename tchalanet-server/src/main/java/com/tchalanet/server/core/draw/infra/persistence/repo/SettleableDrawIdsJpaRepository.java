package com.tchalanet.server.core.draw.infra.persistence.repo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettleableDrawIdsJpaRepository extends JpaRepository<com.tchalanet.server.core.draw.infra.persistence.DrawJpaEntity, UUID> {

  @Query(
      value =
          "select d.id "
              + "from draw d "
              + "join draw_channel dc on dc.id = d.draw_channel_id "
              + "left join draw_result dr on dr.tenant_id = d.tenant_id and dr.draw_id = d.id "
              + "where (:tenantId is null or d.tenant_id = :tenantId) "
              + "  and d.deleted_at is null and dc.deleted_at is null "
              + "  and d.locked = false "
              + "  and d.status in ('RESULTED','PENDING') "
              + "  and d.scheduled_at >= :fromTs and d.scheduled_at < :toTs "
              + "  and (:channelCode is null or dc.code = :channelCode) "
              + "  and (:provider is null or dc.external_provider = :provider) "
              + "  and (:source is null or d.draw_source = :source) "
              + "  and (:force = true or dr.id is null) "
              + "order by d.scheduled_at asc "
              + "limit coalesce(:maxDraws, 1000000000)",
      nativeQuery = true)
  List<UUID> findSettleableDrawIds(
      @Param("tenantId") UUID tenantId,
      @Param("source") String source,
      @Param("provider") String provider,
      @Param("channelCode") String channelCode,
      @Param("fromTs") Instant fromTs,
      @Param("toTs") Instant toTs,
      @Param("maxDraws") Long maxDraws,
      @Param("force") boolean force);
}

