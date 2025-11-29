package com.tchalanet.server.core.draw.application.port.in.command;

import com.tchalanet.server.core.draw.application.command.model.GenerateDrawsForDateRangeCommand;

/** Handler (port in) to generate draws for a tenant within a date range. */
public interface GenerateDrawsForDateRangeCommandHandler {
  void handle(GenerateDrawsForDateRangeCommand command);
}
