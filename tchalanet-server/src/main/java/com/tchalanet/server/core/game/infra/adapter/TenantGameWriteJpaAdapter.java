package com.tchalanet.server.core.game.infra.adapter;

import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.game.application.command.model.UpdateTenantGameCommand;
import com.tchalanet.server.core.game.application.port.out.TenantGameWritePort;
import com.tchalanet.server.core.game.infra.persistence.TenantGameJpaEntity;
import com.tchalanet.server.core.game.infra.persistence.TenantGameJpaRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TenantGameWriteJpaAdapter implements TenantGameWritePort {

  private final TenantGameJpaRepository repo;
  private final JsonUtils jsonUtils;

  @Override
  public boolean updateByGameId(UUID gameId, UpdateTenantGameCommand cmd) {
    Optional<TenantGameJpaEntity> opt = repo.findByGame_IdAndDeletedAtIsNull(gameId);
    if (opt.isEmpty()) return false;
    TenantGameJpaEntity entity = opt.get();

    if (cmd.enabled() != null) entity.setEnabled(cmd.enabled());
    if (cmd.displayName() != null)
      entity.setDisplayName(
          cmd.displayName().trim().substring(0, Math.min(128, cmd.displayName().trim().length())));
    if (cmd.minStake() != null) entity.setMinStake(cmd.minStake());
    if (cmd.maxStake() != null) entity.setMaxStake(cmd.maxStake());
    if (cmd.flags() != null) {
      // convert value using JsonUtils -> map via serialization roundtrip
      @SuppressWarnings("unchecked")
      Map<String, Object> flags =
          (Map<String, Object>) jsonUtils.readValue(jsonUtils.toJson(cmd.flags()), Map.class);
      entity.setFlags(flags);
    }

    repo.save(entity);
    return true;
  }
}
