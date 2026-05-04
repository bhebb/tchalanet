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

    @Query(
        value = """
          select d.id
          from draw d
          where d.deleted_at is null
            and d.tenant_id = :tenantId
            and d.locked = false
            and d.status = 'RESULTED'
            and d.draw_result_id is not null
            and d.resulted_at >= :fromTs
            and d.resulted_at < :toTs
            and (
              :force = true
              or d.settled_at is null
            )
          order by d.resulted_at asc
          limit coalesce(:maxDraws, 1000000000)
          """,
        nativeQuery = true)
    List<UUID> findSettleableDrawIds(
        @Param("tenantId") UUID tenantId,
        @Param("fromTs") Timestamp fromTs,
        @Param("toTs") Timestamp toTs,
        @Param("maxDraws") Long maxDraws,
        @Param("force") boolean force
    );
}
