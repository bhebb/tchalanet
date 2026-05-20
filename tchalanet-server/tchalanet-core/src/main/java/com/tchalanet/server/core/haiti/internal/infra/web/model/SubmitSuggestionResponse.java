package com.tchalanet.server.core.haiti.internal.infra.web.model;

import java.util.UUID;

/** Web response returned after submitting a suggestion. */
public record SubmitSuggestionResponse(
    UUID entryId, String status, boolean conflictsWithCanonical, UUID conflictWithEntryId) {}
