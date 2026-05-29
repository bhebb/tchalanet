package com.tchalanet.server.core.outlet.api.command.lifecycle;

import java.time.LocalDate;
import lombok.Value;

/** Payload for closing an outlet day (application level) */
@Value
public class CloseOutletDayPayload {
  LocalDate from;
  LocalDate to;
  CloseDayMode mode;
  String reason;
}
