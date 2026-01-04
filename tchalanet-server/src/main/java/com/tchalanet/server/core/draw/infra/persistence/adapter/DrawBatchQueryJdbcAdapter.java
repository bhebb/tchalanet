package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.core.draw.application.port.out.DrawBatchQueryPort;
import com.tchalanet.server.core.draw.infra.persistence.repo.ApplyCandidateDrawJdbcRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawBatchQueryJdbcAdapter implements DrawBatchQueryPort {

  private final ApplyCandidateDrawJdbcRepository repo;

  @Override
  public java.util.List<ClosedDrawRef> findClosedDrawsForDate(
      com.tchalanet.server.common.types.id.TenantId tenantIdOrNull,
      LocalDate drawDate,
      List<String> channelCodes,
      int maxDraws) {
    if (channelCodes == null || channelCodes.isEmpty()) return List.of();
    var ids = repo.findClosedDrawIds(drawDate, channelCodes, maxDraws);
    return ids.stream()
        .map(
            id -> {
              var key = repo.findDrawKey(id);
              return new ClosedDrawRef(id, key.channelCode());
            })
        .toList();
  }
}
