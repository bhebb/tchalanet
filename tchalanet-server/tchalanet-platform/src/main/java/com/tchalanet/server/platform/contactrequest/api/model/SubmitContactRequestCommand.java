package com.tchalanet.server.platform.contactrequest.api.model;

import com.tchalanet.server.platform.contactrequest.api.ContactRequestIntent;

public record SubmitContactRequestCommand(
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
    String sourcePage
) {}
