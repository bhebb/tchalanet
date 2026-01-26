package com.tchalanet.server.catalog.drawchannel.internal.persistence;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DrawChannelGameRepository extends JpaRepository<DrawChannelGameEntity, UUID> {
    // RLS: do NOT accept tenantId or filter by deleted_at in repository methods. Tenant scoping
    // and deleted visibility must be applied by PostgreSQL RLS (set via set_config on the connection).
    //
    // The read-side implementation (DrawChannelCatalogImpl) expects a few helper queries. Expose
    // only those with RLS-friendly signatures (no tenant args). Keep the interface minimal to
    // avoid unused-method warnings.

    List<DrawChannelGameEntity> findByDrawChannelId(UUID drawChannelId);

    @Query(value = """
        SELECT dc.code, g.code AS game_code, dcg.game_id, dcg.enabled, dcg.flags
        FROM draw_channel dc
        JOIN draw_channel_game dcg ON dc.id = dcg.draw_channel_id
        JOIN game g ON g.id = dcg.game_id
        """, nativeQuery = true)
    List<Object[]> findChannelCodeAndGameRows();
}
