package com.tchalanet.server.core.draw.infra.persistence.repo;

import com.tchalanet.server.core.draw.infra.persistence.DrawChannelGameJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DrawChannelGameJpaRepository
    extends JpaRepository<DrawChannelGameJpaEntity, UUID> {

  @Query(
      "select e from DrawChannelGameJpaEntity e where e.drawChannelId = :channelId and e.enabled = true")
  List<DrawChannelGameJpaEntity> findEnabledByChannelId(@Param("drawChannelId") UUID channelId);

  @Query(
      "select e from DrawChannelGameJpaEntity e where e.drawChannelId in :channelIds and e.enabled = true")
  List<DrawChannelGameJpaEntity> findEnabledByChannelIds(
      @Param("channelIds") List<UUID> channelIds);
}
