package com.tchalanet.server.platform.contactrequest.api.model;

import com.tchalanet.server.platform.contactrequest.api.ContactRequestIntent;
import com.tchalanet.server.platform.contactrequest.api.ContactRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record ContactRequestAdminDetailView(
    UUID id,
    String reference,
    ContactRequestIntent intent,
    String fullName,
    String phone,
    String email,
    String organizationName,
    String city,
    String country,
    Integer outletCount,
    String preferredContactTime,
    String message,
    boolean consentToContact,
    ContactRequestStatus status,
    String internalNotes,
    String externalTool,
    String externalReference,
    Instant exportedAt,
    String sourcePage,
    Instant createdAt,
    Instant updatedAt
) {}
