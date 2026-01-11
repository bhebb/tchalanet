package com.tchalanet.server.core.draw.application.port.out;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.id.DrawChannelId;

public interface DrawChannelGameWriterPort {
  void upsert(DrawChannelId channelId, String gameCode, boolean enabled, JsonNode flags);
}
