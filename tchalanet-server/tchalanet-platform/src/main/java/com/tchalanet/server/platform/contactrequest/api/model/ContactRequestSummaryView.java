package com.tchalanet.server.platform.contactrequest.api.model;

import com.tchalanet.server.platform.contactrequest.api.ContactRequestIntent;
import com.tchalanet.server.platform.contactrequest.api.ContactRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record ContactRequestSummaryView(
    UUID id,
    String reference,
    ContactRequestIntent intent,
    String fullName,
    String phone,
    String email,
    String city,
    String country,
    ContactRequestStatus status,
    Instant createdAt
) {}
