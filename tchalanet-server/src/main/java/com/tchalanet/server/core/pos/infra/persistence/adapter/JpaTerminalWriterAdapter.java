package com.tchalanet.server.core.pos.infra.persistence.adapter;

import com.tchalanet.server.core.pos.application.port.out.TerminalWriterPort;
import com.tchalanet.server.core.pos.domain.model.Terminal;
import com.tchalanet.server.core.pos.infra.persistence.TerminalJpaRepository;
import com.tchalanet.server.core.pos.infra.persistence.mapper.TerminalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaTerminalWriterAdapter implements TerminalWriterPort {

  private final TerminalJpaRepository jpaRepository;
  private final TerminalMapper mapper;

  @Override
  public Terminal save(Terminal terminal) {
    var entity = mapper.toEntity(terminal);
    var saved = jpaRepository.save(entity);
    return mapper.toDomain(saved);
  }
}
