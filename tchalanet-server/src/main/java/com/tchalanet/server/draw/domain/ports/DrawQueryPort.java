package com.tchalanet.server.draw.domain.ports;

import com.tchalanet.server.draw.domain.dto.DrawDto;
import com.tchalanet.server.draw.domain.dto.NextDrawDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DrawQueryPort {
  List<DrawDto> findResultedDrawsForLastDays(UUID tenantId, int days);

  Map<UUID, NextDrawDto> findNextDrawPerChannel(UUID tenantId);

  // returns per-channel lists limited to perChannelCount (e.g. last 7 draws per channel)
  Map<UUID, List<DrawDto>> findLastNPerChannel(UUID tenantId, int perChannelCount);
}
