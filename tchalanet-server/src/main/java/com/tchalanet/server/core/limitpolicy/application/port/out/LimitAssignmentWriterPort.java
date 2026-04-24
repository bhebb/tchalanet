package com.tchalanet.server.core.limitpolicy.application.port.out;

import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.common.types.id.LimitDefinitionId;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;

public interface LimitAssignmentWriterPort {
  LimitAssignment save(LimitAssignment assignment);
  void softDelete(LimitAssignmentId id);
  void softDeleteByDefinitionId(LimitDefinitionId defId);
}
