package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.core.draw.application.port.out.DrawBatchQueryPort;
import com.tchalanet.server.core.draw.infra.persistence.repo.ApplyCandidateDrawJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawBatchQueryJdbcAdapter implements DrawBatchQueryPort {

  private final ApplyCandidateDrawJdbcRepository repo;
}
