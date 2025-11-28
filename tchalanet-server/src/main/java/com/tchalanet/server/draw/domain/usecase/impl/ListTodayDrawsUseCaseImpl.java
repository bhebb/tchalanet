package com.tchalanet.server.draw.domain.usecase.impl;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.draw.domain.dto.DrawDto;
import com.tchalanet.server.draw.domain.ports.DrawRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

@UseCase
@RequiredArgsConstructor
public class ListTodayDrawsUseCaseImpl
    implements com.tchalanet.server.draw.domain.usecase.ListTodayResultsUseCase {

  private final StringRedisTemplate redis;
  private final DrawRepository drawRepository;

  @Override
  public List<Map<String, DrawDto>> listTodayResults(UUID tenantId) {
    // Convert date to start/end instants UTC
    LocalDate date = LocalDate.now();
    Instant start = date.atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    var draws = drawRepository.findByTenantAndScheduledAtBetween(tenantId, start, end);
    return null;
  }
}
