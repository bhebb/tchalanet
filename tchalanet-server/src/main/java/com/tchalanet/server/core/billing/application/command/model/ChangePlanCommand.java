package com.tchalanet.server.core.billing.application.command.model;

import java.util.UUID;

import com.tchalanet.server.common.bus.Command;

import com.tchalanet.server.core.billing.domain.model.Subscription;

public record ChangePlanCommand(UUID subscriptionId, UUID tenantId, UUID planId,
                                boolean proration) implements Command<Subscription> {
}
