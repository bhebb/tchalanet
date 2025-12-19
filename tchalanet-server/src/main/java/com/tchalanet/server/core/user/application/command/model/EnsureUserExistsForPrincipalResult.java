package com.tchalanet.server.core.user.application.command.model;

import java.util.UUID;

/**
 * Résultat neutre (pas DTO web) pour EnsureUserExistsForPrincipal.
 */
public record EnsureUserExistsForPrincipalResult(
    boolean isNew,
    UUID userId
) {
}

