package com.tchalanet.server.core.game.infra.persistence;

import com.tchalanet.server.core.audit.application.command.model.LogAuditEventCommand;
import com.tchalanet.server.core.audit.application.port.in.LogAuditEventCommandHandler;
import com.tchalanet.server.core.audit.domain.model.AuditAction;
import com.tchalanet.server.core.audit.domain.model.AuditEntityType;
import com.tchalanet.server.core.game.domain.model.Game;
import com.tchalanet.server.core.game.domain.ports.GameRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameRepositoryAdapter implements GameRepository {

  private final GameJpaRepository repo;
  private final LogAuditEventCommandHandler auditLog;

  @Override
  public Game save(Game g) {
    GameJpaEntity e = GameMapper.toEntity(g);
    GameJpaEntity saved = repo.save(e);
    return GameMapper.toDomain(saved);
  }

  @Override
  public Optional<Game> findByCode(String code) {
    return repo.findByCode(code).map(GameMapper::toDomain);
  }

  @Override
  public List<Game> findAllActive() {
    return repo.findByActiveTrueOrderBySortOrder().stream()
        .map(GameMapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public void softDeleteByCode(String code) {
    repo.findByCode(code)
        .ifPresent(
            e -> {
              e.setDeletedAt(java.time.Instant.now());
              repo.save(e);
              var details = Map.<String, Object>of("code", e.getCode());
              auditLog.handle(
                  new LogAuditEventCommand(
                      AuditEntityType.GAME,
                      e.getId().toString(),
                      AuditAction.SOFT_DELETE,
                      details));
            });
  }
}
