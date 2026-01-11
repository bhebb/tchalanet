package com.tchalanet.server.core.haiti.application.command.model;

import com.tchalanet.server.common.bus.Command;

public record ImportTchalaEntriesCommand(String lang, String payloadRef, ImportMode mode)
    implements Command<ImportTchalaReport> {
  public enum ImportMode {
    DRY_RUN,
    APPLY_AS_PENDING,
    APPLY_AS_APPROVED
  }

  public static record ImportRow(String dream, String numbers, String note) {}
}
