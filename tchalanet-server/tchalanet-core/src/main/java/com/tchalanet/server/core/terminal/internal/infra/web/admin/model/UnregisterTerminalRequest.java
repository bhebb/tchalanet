package com.tchalanet.server.core.terminal.internal.infra.web.admin.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.core.terminal.domain.model.TerminalKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record UnregisterTerminalRequest(@NotBlank String reason) {}
