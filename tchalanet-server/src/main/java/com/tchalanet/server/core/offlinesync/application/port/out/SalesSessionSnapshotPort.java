package com.tchalanet.server.core.offlinesync.application.port.out;

import com.tchalanet.server.common.types.id.SalesSessionId;
import java.time.Instant;

public interface SalesSessionSnapshotPort {
  SalesSessionSnapshot getSnapshot(SalesSessionId sessionId);

  record SalesSessionSnapshot(String status, Instant closedAt, boolean finalized) {}
}

