package com.tchalanet.server.core.notification.application.command.model;

import com.tchalanet.server.common.bus.Command;
import java.time.Instant;

public record ExpireNotificationsCommand(Instant now) implements Command<Integer> {}
