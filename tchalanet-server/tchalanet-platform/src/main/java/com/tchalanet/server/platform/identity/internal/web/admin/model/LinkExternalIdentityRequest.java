package com.tchalanet.server.platform.identity.internal.web.admin.model;

import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LinkExternalIdentityRequest(
    @NotNull IdentityProviderType provider,
    @NotBlank @Size(max = 512) String issuer,
    @NotBlank @Size(max = 255) String externalSubject,
    @Email String emailSnapshot) {}

