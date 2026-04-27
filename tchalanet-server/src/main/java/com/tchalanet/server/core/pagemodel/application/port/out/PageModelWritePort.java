package com.tchalanet.server.core.pagemodel.application.port.out;

import tools.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelInstance;
import java.util.List;

public interface PageModelWritePort {
  PageModelInstance save(PageModelInstance instance);
  List<PageModelInstance> saveAll(List<PageModelInstance> instances);

  // [Phase 4C] propagation template → instances DRAFT (analysis §gap — PageModelTemplateUpdatedEvent)
  void applyTemplateUpdate(PageModelTemplateId templateId, JsonNode newModel,
      int newSchemaVersion, UserId actorId);
}
