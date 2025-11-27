package com.tchalanet.server.pos.infra.persistence.adapter;

import com.tchalanet.server.pos.domain.model.PosSession;
import com.tchalanet.server.pos.domain.model.PosSessionStatus;
import com.tchalanet.server.pos.domain.ports.out.PosSessionRepositoryPort;
import com.tchalanet.server.pos.infra.persistence.entity.PosSessionEntity;
import com.tchalanet.server.pos.infra.persistence.mapper.PosSessionMapper;
import com.tchalanet.server.pos.infra.persistence.repository.SpringPosSessionJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaPosSessionRepositoryAdapter implements PosSessionRepositoryPort {

  private final SpringPosSessionJpaRepository jpaRepository;
  private final PosSessionMapper mapper;

  @Override
  public PosSession save(PosSession session) {
    PosSessionEntity entity = mapper.toEntity(session);
    PosSessionEntity savedEntity = jpaRepository.save(entity);
    return mapper.toDomain(savedEntity);
  }

  @Override
  public Optional<PosSession> findById(UUID sessionId) {
    return jpaRepository.findById(sessionId).map(mapper::toDomain);
  }

  @Override
  public Optional<PosSession> findOpenSessionByTerminal(UUID tenantId, UUID terminalId) {
    return jpaRepository
        .findByTenantIdAndTerminalIdAndStatus(tenantId, terminalId, PosSessionStatus.OPEN)
        .map(mapper::toDomain);
  }

  @Override
  public List<PosSession> findOpenSessions(Instant idleCutoff, Instant openedCutoff) {
    return jpaRepository.findOpenSessionsToAutoClose(idleCutoff, openedCutoff).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }
}
