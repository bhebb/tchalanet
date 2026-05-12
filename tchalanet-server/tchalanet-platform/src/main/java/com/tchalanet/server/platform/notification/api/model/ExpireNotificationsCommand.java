package com.tchalanet.server.platform.notification.api.model;

import com.tchalanet.server.common.bus.Command;
import java.time.Instant;

public record ExpireNotificationsCommand(Instant now) implements Command<Integer> {}
