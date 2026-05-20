package com.tchalanet.server.core.haiti.api.command;

public record SubmitTchalaSuggestionResult(
    java.util.UUID entryId,
    String status,
    boolean conflictsWithCanonical,
    java.util.UUID conflictWithEntryId) {}
