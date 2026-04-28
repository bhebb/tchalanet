package com.tchalanet.server.core.terminal.infra.persistence.adapter;

import com.tchalanet.server.core.terminal.application.port.out.TerminalWriterPort;
import com.tchalanet.server.core.terminal.domain.model.Terminal;
import com.tchalanet.server.core.terminal.infra.persistence.TerminalJpaRepository;
import com.tchalanet.server.core.terminal.infra.persistence.mapper.TerminalMapper;
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
