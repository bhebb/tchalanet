package com.tchalanet.server.core.billing.application.command.model;

import com.tchalanet.server.common.bus.Command;

public record RenewSubscriptionsCommand() implements Command<Void> {}
