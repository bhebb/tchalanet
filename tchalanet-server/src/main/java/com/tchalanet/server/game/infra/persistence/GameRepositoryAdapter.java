package com.tchalanet.server.game.infra.persistence;

import com.tchalanet.server.audit.domain.model.AuditAction;
import com.tchalanet.server.audit.domain.model.AuditActorType;
import com.tchalanet.server.audit.domain.model.AuditEntityType;
import com.tchalanet.server.audit.domain.model.AuditEvent;
import com.tchalanet.server.audit.domain.usecase.LogAuditEventUseCase;
import com.tchalanet.server.game.domain.model.Game;
import com.tchalanet.server.game.domain.ports.GameRepository;
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
  private final LogAuditEventUseCase auditLog;

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
              var ev =
                  AuditEvent.of(
                      null,
                      AuditActorType.SYSTEM,
                      "system",
                      AuditEntityType.GAME,
                      e.getId().toString(),
                      AuditAction.SOFT_DELETE,
                      Map.of("code", e.getCode()).toString(),
                      null,
                      null);
              auditLog.log(ev);
            });
  }
}
