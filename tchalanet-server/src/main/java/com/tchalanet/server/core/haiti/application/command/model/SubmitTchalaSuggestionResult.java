package com.tchalanet.server.core.haiti.application.command.model;

public record SubmitTchalaSuggestionResult(
    java.util.UUID entryId,
    String status,
    boolean conflictsWithCanonical,
    java.util.UUID conflictWithEntryId) {}
