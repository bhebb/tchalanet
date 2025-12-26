package com.tchalanet.server.core.draw.infra.persistence.repo;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public interface FetchableDrawRow {
  UUID getDrawId();

  UUID getTenantId();

  Instant getScheduledAt();

  String getChannelCode();

  String getExternalProvider();

  String getExternalGameKey();

  String getExternalChannelCode();

  String getTimezone();
}

