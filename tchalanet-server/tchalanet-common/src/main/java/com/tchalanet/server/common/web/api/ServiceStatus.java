package com.tchalanet.server.common.web.api;

import com.tchalanet.server.common.web.api.ServiceHealth;

/** Represents the status of a downstream service. */
public record ServiceStatus(String service, ServiceHealth status, String message) {}
