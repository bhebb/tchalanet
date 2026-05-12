package com.tchalanet.server.core.pagemodel.internal.application.port.out;

import tools.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelInstance;
import java.util.List;

public interface PageModelWritePort {
  PageModelInstance save(PageModelInstance instance);
  List<PageModelInstance> saveAll(List<PageModelInstance> instances);

  void applyTemplateUpdate(PageModelTemplateId templateId, String logicalId, JsonNode newModel,
      int newSchemaVersion, UserId actorId);
}
