package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.bus.Command;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CloseDueDrawsCommand(@NotNull Instant now, int limit, boolean dryRun)
implements Command<CloseDueDrawsResult> {}
