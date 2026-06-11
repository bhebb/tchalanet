package com.tchalanet.server.platform.contactrequest.internal.web.model;

import com.tchalanet.server.platform.contactrequest.api.ContactRequestIntent;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitContactRequestHttpRequest(

    @NotNull
    ContactRequestIntent intent,

    @NotBlank @Size(max = 160)
    String fullName,

    @NotBlank @Size(max = 64)
    String phone,

    @Email @Size(max = 160)
    String email,

    @Size(max = 180)
    String organizationName,

    @Size(max = 120)
    String city,

    @Size(max = 120)
    String country,

    @Min(0)
    Integer outletCount,

    @Size(max = 120)
    String preferredContactTime,

    @NotBlank
    String message,

    @AssertTrue(message = "Le consentement est requis")
    boolean consentToContact,

    @Size(max = 160)
    String sourcePage
) {}
