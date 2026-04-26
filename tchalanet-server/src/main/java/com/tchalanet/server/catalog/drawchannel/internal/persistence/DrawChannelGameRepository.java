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
        SELECT dc.code, g.code AS game_code, dcg.game_id, dcg.enabled, dcg.flags
        FROM draw_channel dc
        JOIN draw_channel_game dcg ON dc.id = dcg.draw_channel_id
        JOIN game g ON g.id = dcg.game_id
        """, nativeQuery = true)
    List<Object[]> findChannelCodeAndGameRows();

    // Admin/Write methods (using explicit tenantId for double-check security)
    Optional<DrawChannelGameEntity> findByTenantIdAndDrawChannelIdAndGameIdAndDeletedAtIsNull(UUID tenantId, UUID drawChannelId, UUID gameId);

    List<DrawChannelGameEntity> findByTenantIdAndDrawChannelIdAndGameIdInAndDeletedAtIsNull(UUID tenantId, UUID drawChannelId, List<UUID> gameIds);
}
