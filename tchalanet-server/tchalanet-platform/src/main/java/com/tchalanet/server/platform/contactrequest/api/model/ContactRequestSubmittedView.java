package com.tchalanet.server.platform.contactrequest.api.model;

public record ContactRequestSubmittedView(
    String requestId,
    String status,
    String message
) {}
