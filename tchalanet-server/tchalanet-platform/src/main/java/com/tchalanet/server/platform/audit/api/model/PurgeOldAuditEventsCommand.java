package com.tchalanet.server.platform.audit.api.model;

import com.tchalanet.server.common.bus.Command;

public record PurgeOldAuditEventsCommand() implements Command<PurgeOldAuditEventsResult> {}
