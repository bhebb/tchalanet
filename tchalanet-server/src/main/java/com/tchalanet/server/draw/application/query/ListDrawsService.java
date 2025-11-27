package com.tchalanet.server.draw.application.query;

import com.tchalanet.server.draw.application.dto.ChannelSummary;
import com.tchalanet.server.draw.application.dto.DrawSummary;
import com.tchalanet.server.draw.application.ports.in.query.ListDrawsQuery;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListDrawsService implements ListDrawsQuery {

  // private final DrawRepository drawRepository; // Placeholder
  // private final DrawChannelRepository drawChannelRepository; // Placeholder

  @Override
  public List<ChannelSummary> listTodayDraws(UUID tenantId) {
    log.warn("ListDrawsService.listTodayDraws is a placeholder.");
    return List.of();
  }

  @Override
  public List<ChannelSummary> listTodayResults(UUID tenantId) {
    log.warn("ListDrawsService.listTodayResults is a placeholder.");
    return List.of();
  }

  @Override
  public List<ChannelSummary> listLast7DaysResults(UUID tenantId) {
    log.warn("ListDrawsService.listLast7DaysResults is a placeholder.");
    return List.of();
  }

  @Override
  public Optional<DrawSummary> getPublicDrawSummary(UUID tenantId) {
    log.warn("ListDrawsService.getPublicDrawSummary is a placeholder.");
    return Optional.empty();
  }

  @Override
  public List<ChannelSummary> getNextDraws(UUID tenantId, int limit) {
    log.warn("ListDrawsService.getNextDraws is a placeholder.");
    return List.of();
  }
}
