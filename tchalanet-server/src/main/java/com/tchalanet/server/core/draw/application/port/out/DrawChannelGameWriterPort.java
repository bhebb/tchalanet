package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.common.types.id.DrawChannelId;
import tools.jackson.databind.JsonNode;

public interface DrawChannelGameWriterPort {
  void upsert(DrawChannelId channelId, String gameCode, boolean enabled, JsonNode flags);
}
