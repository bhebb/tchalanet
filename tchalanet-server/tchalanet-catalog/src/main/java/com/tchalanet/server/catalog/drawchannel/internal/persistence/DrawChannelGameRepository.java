package com.tchalanet.server.catalog.drawchannel.internal.persistence;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DrawChannelGameRepository extends JpaRepository<DrawChannelGameEntity, UUID> {

    List<DrawChannelGameEntity> findByDrawChannelId(UUID drawChannelId);

    @Query(value = """
        SELECT dc.code, g.code AS game_code, dcg.tenant_game_id, dcg.enabled, dcg.flags
        FROM draw_channel dc
        JOIN draw_channel_game dcg ON dc.id = dcg.draw_channel_id
        JOIN tenant_game tg ON tg.id = dcg.tenant_game_id
        JOIN game g ON g.id = tg.game_id
        WHERE dcg.deleted_at IS NULL
        """, nativeQuery = true)
    List<Object[]> findChannelCodeAndGameRows();

    @Query(value = """
        SELECT dc.code, g.code AS game_code, dcg.tenant_game_id, tg.id AS tg_id,
               dcg.draw_channel_id, dc.result_slot_id, dc.timezone, dc.draw_time,
               dc.sales_open_time, dc.cutoff_sec, dc.days_of_week, NULL AS default_source,
               dc.active AS channel_active, dcg.enabled AS dcg_enabled, dc.sort_order,
               NULL AS depends_on_channel_id
        FROM draw_channel dc
        JOIN draw_channel_game dcg ON dc.id = dcg.draw_channel_id
        JOIN tenant_game tg ON tg.id = dcg.tenant_game_id
        JOIN game g ON g.id = tg.game_id
        WHERE dcg.deleted_at IS NULL
        """, nativeQuery = true)
    List<Object[]> findCalendarRows();

    // Admin/Write methods (using explicit tenantId for double-check security)
    Optional<DrawChannelGameEntity> findByTenantIdAndDrawChannelIdAndTenantGameIdAndDeletedAtIsNull(UUID tenantId, UUID drawChannelId, UUID tenantGameId);

    List<DrawChannelGameEntity> findByTenantIdAndDrawChannelIdAndTenantGameIdInAndDeletedAtIsNull(UUID tenantId, UUID drawChannelId, List<UUID> tenantGameIds);
}
