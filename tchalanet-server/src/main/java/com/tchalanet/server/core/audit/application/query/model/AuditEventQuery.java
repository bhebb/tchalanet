package com.tchalanet.server.core.audit.application.query.model;

import java.util.UUID;

public record AuditEventQuery(UUID tenant, Integer limit) {}
