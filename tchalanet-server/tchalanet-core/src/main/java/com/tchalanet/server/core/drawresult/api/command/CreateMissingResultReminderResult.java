package com.tchalanet.server.core.drawresult.api.command;

public record CreateMissingResultReminderResult(
    String dedupeKey, boolean created, boolean skippedResultAlreadyPresent) {}
