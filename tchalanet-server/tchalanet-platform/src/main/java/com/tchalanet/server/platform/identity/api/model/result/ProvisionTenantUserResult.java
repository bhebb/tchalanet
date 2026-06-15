package com.tchalanet.server.platform.identity.api.model.result;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;

/**
 * Result of provisioning an initial tenant user.
 *
 * @param userId DB user ID (app_user.id)
 * @param provider configured external identity provider
 * @param externalSubject provider-owned stable subject
 * @param externalIdentityCreated true if a new external identity was created
 */
public record ProvisionTenantUserResult(
    UserId userId,
    IdentityProviderType provider,
    String externalSubject,
    boolean externalIdentityCreated) {}
