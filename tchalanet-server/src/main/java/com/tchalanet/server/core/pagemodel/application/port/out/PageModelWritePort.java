package com.tchalanet.server.core.pagemodel.application.port.out;

import java.util.UUID;

public interface PageModelWritePort {

  UUID save(PageModelMutation mutation);

  void softDelete(UUID id, UUID actorId);

  void archivePublishedForLogicalId(String logicalId, UUID actorId);

  // Mutation record placeholder
  public static class PageModelMutation {
    // minimal placeholder
  }
}
