package com.tchalanet.server.core.session.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.application.port.out.SalesSessionReaderPort;
import com.tchalanet.server.core.session.application.port.out.SalesSessionWriterPort;
import com.tchalanet.server.core.session.domain.model.SalesSession;
import com.tchalanet.server.core.session.infra.persistence.mapper.SalesSessionMapper;
import com.tchalanet.server.core.session.infra.persistence.repository.SalesSessionJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Adapter for SalesSessionReaderPort and SalesSessionWriterPort using JPA. */
@Component
@RequiredArgsConstructor
public class SalesSessionRepositoryAdapter implements SalesSessionReaderPort, SalesSessionWriterPort {

  private final SalesSessionJpaRepository jpaRepository;

  private final SalesSessionMapper mapper;

  @Override
  public Optional<SalesSession> findById(SessionId id) {
    return jpaRepository.findById(id.uuid()).map(mapper::toDomain);
  }

  @Override
  public Optional<SalesSession> findOpenByTerminal(TenantId tenantId, TerminalId terminalId) {
    return jpaRepository
        .findByTenantIdAndTerminalIdAndStatus(
            tenantId.uuid(),
            terminalId.uuid(),
            com.tchalanet.server.core.session.domain.model.SalesSessionStatus.OPENED)
        .map(mapper::toDomain);
  }

  @Override
  public SalesSession save(SalesSession session) {
    var entity = mapper.toEntity(session);
    var savedEntity = jpaRepository.save(entity);
    return mapper.toDomain(savedEntity);
  }

  @Override
  public List<SalesSession> findOpenByCashier(TenantId tenantId, UserId userId) {
    return jpaRepository.findOpenByCashier(tenantId.uuid(), userId.uuid()).stream()
        .map(mapper::toDomain)
        .toList();
  }
}
