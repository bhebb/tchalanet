package com.tchalanet.server.core.pos.infra.persistence.adapter;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;

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
  public Optional<Terminal> findById( TenantId tenantId,  TerminalId terminalId) {
    return jpaRepository.findByTenantIdAndId(tenantId.uuid(), terminalId.uuid())
        .map(mapper::toDomain);
  }

  @Override
  public List<Terminal> listByOutlet( TenantId tenantId,  OutletId outletId, PageRequest pageRequest) {
    return jpaRepository.findAllByTenantIdAndOutletIdAndDeletedAtIsNull(tenantId.uuid(), outletId.uuid(), pageRequest)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }
}
