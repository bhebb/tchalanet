package com.tchalanet.server.core.tenant.application.command.handler;

import com.tchalanet.server.common.app.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.tenant.application.command.model.ArchiveTenantCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class ArchiveTenantCommandHandler implements VoidCommandHandler<ArchiveTenantCommand> {

  @Override
  public void handle(ArchiveTenantCommand command) {
    // TODO: implement soft delete (set archived_at)
    throw new UnsupportedOperationException("ArchiveTenantCommandHandler not implemented yet");
  }
}

