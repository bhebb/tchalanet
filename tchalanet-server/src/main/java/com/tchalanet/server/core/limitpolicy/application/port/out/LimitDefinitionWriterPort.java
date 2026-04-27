package com.tchalanet.server.core.limitpolicy.application.port.out;

import com.tchalanet.server.common.types.id.LimitDefinitionId;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitDefinition;

public interface LimitDefinitionWriterPort {
  LimitDefinition save(LimitDefinition def);

  void softDelete(LimitDefinitionId id);
}
