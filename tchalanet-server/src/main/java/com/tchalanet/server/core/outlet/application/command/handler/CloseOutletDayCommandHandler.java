package com.tchalanet.server.core.outlet.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.application.command.model.CloseOutletDayCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class CloseOutletDayHandler implements VoidCommandHandler<CloseOutletDayCommand> {

  @Override
  public void handle(CloseOutletDayCommand command) {
    // TODO: implement closing day (ledger, sessions, snapshots)
    throw new UnsupportedOperationException("CloseOutletDayHandler not implemented yet");
  }
}

