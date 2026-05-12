package com.tchalanet.server.platform.accesscontrol.internal.service;

import jakarta.validation.constraints.NotNull;

/** Value Object représentant une permission métier. Exemple : "ticket.create", "draw.override". */
public record Permission(@NotNull String code) {}
