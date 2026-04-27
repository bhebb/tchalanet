package com.tchalanet.server.core.limitpolicy.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.application.command.model.DeleteLimitDefinitionCommand;
import com.tchalanet.server.core.limitpolicy.application.command.model.DeleteLimitDefinitionResult;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitAssignmentWriterPort;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitDefinitionWriterPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeleteLimitDefinitionCommandHandler
    implements CommandHandler<DeleteLimitDefinitionCommand, DeleteLimitDefinitionResult> {

  private final LimitDefinitionWriterPort defWriter;
  private final LimitAssignmentWriterPort asgWriter;

  @Override
  @TchTx
  public DeleteLimitDefinitionResult handle(DeleteLimitDefinitionCommand c) {
    // 1) delete assignments first (tenant scoped by RLS)
    asgWriter.softDeleteByDefinitionId(c.id());

    // 2) delete definition
    defWriter.softDelete(c.id());

    return new DeleteLimitDefinitionResult(c.id());
  }
}
