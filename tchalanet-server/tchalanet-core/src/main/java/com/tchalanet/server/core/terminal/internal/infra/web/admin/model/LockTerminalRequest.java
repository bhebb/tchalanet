package com.tchalanet.server.core.terminal.internal.infra.web.admin.model;

import jakarta.validation.constraints.NotBlank;

public record LockTerminalRequest(@NotBlank String reason) {
}
