package com.tchalanet.server.core.game.infra.persistence;

import com.tchalanet.server.core.game.infra.read.TenantGameView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantGameReadRepository extends JpaRepository<TenantGameJpaEntity, UUID> {

  @Query(
      "select new com.tchalanet.server.core.game.infra.read.TenantGameView("
          + "tg.id, g.id, g.code, g.name, g.category, g.minDigits, g.maxDigits, g.combination, "
          + "tg.enabled, tg.displayName, tg.minStake, tg.maxStake, tg.flags) "
          + "from TenantGameJpaEntity tg join tg.game g "
          + "where tg.deletedAt is null "
          + "order by g.sortOrder asc")
  List<TenantGameView> listAllForCurrentTenant();

  @Query(
      "select new com.tchalanet.server.core.game.infra.read.TenantGameView("
          + "tg.id, g.id, g.code, g.name, g.category, g.minDigits, g.maxDigits, g.combination, "
          + "tg.enabled, tg.displayName, tg.minStake, tg.maxStake, tg.flags) "
          + "from TenantGameJpaEntity tg join tg.game g "
          + "where tg.deletedAt is null and tg.enabled = true "
          + "order by g.sortOrder asc")
  List<TenantGameView> listEnabledForCurrentTenant();

  @Query(
      "select new com.tchalanet.server.core.game.infra.read.TenantGameView("
          + "tg.id, g.id, g.code, g.name, g.category, g.minDigits, g.maxDigits, g.combination, "
          + "tg.enabled, tg.displayName, tg.minStake, tg.maxStake, tg.flags) "
          + "from TenantGameJpaEntity tg join tg.game g "
          + "where tg.deletedAt is null and g.code = :code")
  Optional<TenantGameView> findByGameCode(String code);

  @Query(
      "select new com.tchalanet.server.core.game.infra.read.TenantGameView("
          + "tg.id, g.id, g.code, g.name, g.category, g.minDigits, g.maxDigits, g.combination, "
          + "tg.enabled, tg.displayName, tg.minStake, tg.maxStake, tg.flags) "
          + "from TenantGameJpaEntity tg join tg.game g "
          + "where tg.deletedAt is null and g.id = :gameId")
  Optional<TenantGameView> findByGameId(UUID gameId);
}
