package com.tchalanet.server.core.limitpolicy.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.application.command.model.DeleteLimitAssignmentCommand;
import com.tchalanet.server.core.limitpolicy.application.command.model.DeleteLimitAssignmentResult;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitAssignmentWriterPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeleteLimitAssignmentCommandHandler
    implements CommandHandler<DeleteLimitAssignmentCommand, DeleteLimitAssignmentResult> {

  private final LimitAssignmentWriterPort writer;

  @Override
  @TchTx
  public DeleteLimitAssignmentResult handle(DeleteLimitAssignmentCommand c) {
    writer.softDelete(c.id());
    return new DeleteLimitAssignmentResult(c.id());
  }
}
