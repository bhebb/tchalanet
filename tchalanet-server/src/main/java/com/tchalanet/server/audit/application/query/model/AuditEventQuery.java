package com.tchalanet.server.audit.application.query.model;

import java.util.UUID;

public record AuditEventQuery(UUID tenant, Integer limit) {}
