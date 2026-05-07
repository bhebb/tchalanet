package com.tchalanet.server.core.audit.application.command.model;

import com.tchalanet.server.common.bus.Command;

public record PurgeOldAuditEventsCommand() implements Command<PurgeOldAuditEventsResult> {}
