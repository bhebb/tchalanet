package com.tchalanet.server.core.session.infra.persistence.adapter;

import com.tchalanet.server.core.session.application.port.out.PosSessionReaderPort;
import com.tchalanet.server.core.session.application.port.out.PosSessionWriterPort;
import com.tchalanet.server.core.session.domain.model.PosSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.tchalanet.server.core.session.infra.persistence.mapper.PosSessionMapper;
import com.tchalanet.server.core.session.infra.persistence.repository.PosSessionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adapter for PosSessionReaderPort and PosSessionWriterPort using JPA.
 */
@Component
@RequiredArgsConstructor
public class PosSessionRepositoryAdapter implements PosSessionReaderPort, PosSessionWriterPort {

  private final PosSessionJpaRepository jpaRepository;

  private final PosSessionMapper mapper;

  @Override
  public Optional<PosSession> findById(UUID id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<PosSession> findOpenByTerminal(UUID tenantId, UUID terminalId) {
    return jpaRepository.findByTenantIdAndTerminalIdAndStatus(tenantId, terminalId, "OPEN")
        .map(mapper::toDomain);
  }

  @Override
  public PosSession save(PosSession session) {
    var entity = mapper.toEntity(session);
    var savedEntity = jpaRepository.save(entity);
    return mapper.toDomain(savedEntity);
  }

  @Override
  public List<PosSession> findOpenByCashier(UUID tenantId, UUID userId) {
    return jpaRepository.findOpenByCashier(tenantId, userId)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }
}
