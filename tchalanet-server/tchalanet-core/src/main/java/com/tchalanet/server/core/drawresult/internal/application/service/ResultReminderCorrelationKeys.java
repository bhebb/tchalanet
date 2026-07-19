package com.tchalanet.server.core.drawresult.internal.application.service;

import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.core.drawresult.api.model.ResultReminderReason;
import java.time.LocalDate;

public final class ResultReminderCorrelationKeys {

  private ResultReminderCorrelationKeys() {}

  public static String actionRequired(
      ResultReminderReason reason, ResultSlotId resultSlotId, LocalDate drawDate) {
    var reasonPart =
        reason == ResultReminderReason.AUTOMATIC_FETCH_OVERDUE ? "automatic-overdue" : "manual";
    return "drawresult.action-required:%s:%s:%s"
        .formatted(reasonPart, resultSlotId.value(), drawDate);
  }

  public static String manual(ResultSlotId resultSlotId, LocalDate drawDate) {
    return actionRequired(ResultReminderReason.MANUAL_ENTRY_REQUIRED, resultSlotId, drawDate);
  }

  public static String automaticOverdue(ResultSlotId resultSlotId, LocalDate drawDate) {
    return actionRequired(ResultReminderReason.AUTOMATIC_FETCH_OVERDUE, resultSlotId, drawDate);
  }
}
