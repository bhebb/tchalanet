package com.tchalanet.server.core.draw.application.port.out;

import java.util.UUID;

public interface DrawBatchQueryPort {
  record ClosedDrawRef(UUID drawId, String channelCode) {}
}
