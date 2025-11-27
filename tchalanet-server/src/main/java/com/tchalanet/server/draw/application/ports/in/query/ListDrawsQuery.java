package com.tchalanet.server.draw.application.ports.in.query;

import com.tchalanet.server.draw.application.dto.ChannelSummary;
import com.tchalanet.server.draw.application.dto.DrawSummary;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListDrawsQuery {
  List<ChannelSummary> listTodayDraws(UUID tenantId);

  List<ChannelSummary> listTodayResults(UUID tenantId);

  List<ChannelSummary> listLast7DaysResults(UUID tenantId);

  Optional<DrawSummary> getPublicDrawSummary(UUID tenantId);

  List<ChannelSummary> getNextDraws(UUID tenantId, int limit);
}
