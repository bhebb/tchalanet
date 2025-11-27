package com.tchalanet.server.draw.application.ports.in.query;

import com.tchalanet.server.draw.application.dto.ChannelSummary;
import java.util.Optional;
import java.util.UUID;

public interface GetNextDrawQuery {
  Optional<ChannelSummary> getNextDraw(UUID tenantId, String gameCode);
}
