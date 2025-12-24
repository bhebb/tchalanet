package com.tchalanet.server.core.pos.infra.persistence.adapter;

import com.tchalanet.server.core.pos.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.pos.domain.model.Terminal;
import com.tchalanet.server.core.pos.infra.persistence.TerminalJpaRepository;
import com.tchalanet.server.core.pos.infra.persistence.mapper.TerminalMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaTerminalReaderAdapter implements TerminalReaderPort {

  private final TerminalJpaRepository jpaRepository;
  private final TerminalMapper mapper;

  @Override
  public Optional<Terminal> findById(UUID tenantId, UUID terminalId) {
    return jpaRepository.findByTenantIdAndId(tenantId, terminalId)
        .map(mapper::toDomain);
  }

  @Override
  public List<Terminal> listByOutlet(UUID tenantId, UUID outletId, PageRequest pageRequest) {
    return jpaRepository.findAllByTenantIdAndOutletIdAndDeletedAtIsNull(tenantId, outletId, pageRequest)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }
}
