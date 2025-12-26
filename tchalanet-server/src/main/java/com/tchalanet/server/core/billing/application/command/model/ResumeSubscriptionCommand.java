package com.tchalanet.server.core.billing.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.billing.domain.model.Subscription;

public record ResumeSubscriptionCommand(TenantId tenantId) implements Command<Subscription> {}
