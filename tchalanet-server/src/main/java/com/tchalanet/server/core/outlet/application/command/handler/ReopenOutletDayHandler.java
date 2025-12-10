package com.tchalanet.server.core.outlet.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.application.command.model.ReopenOutletDayCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class ReopenOutletDayHandler implements VoidCommandHandler<ReopenOutletDayCommand> {

  @Override
  public void handle(ReopenOutletDayCommand command) {
    // TODO: implement reopen logic
    throw new UnsupportedOperationException("ReopenOutletDayHandler not implemented yet");
  }
}

