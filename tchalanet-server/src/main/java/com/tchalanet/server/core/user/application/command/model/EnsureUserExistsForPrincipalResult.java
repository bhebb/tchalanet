package com.tchalanet.server.core.user.application.command.model;

import com.tchalanet.server.common.types.id.UserId;

/** Résultat neutre (pas DTO web) pour EnsureUserExistsForPrincipal. */
public record EnsureUserExistsForPrincipalResult(boolean isNew, UserId userId) {}
