package com.tchalanet.server.platform.identity.api.model.result;

import com.tchalanet.server.common.types.id.UserId;
import java.util.UUID;

/**
 * Result of provisioning an initial tenant user.
 *
 * @param userId      DB user ID (app_user.id)
 * @param keycloakId  Keycloak user UUID, null if KC bootstrap is disabled
 * @param kcCreated   true if a new Keycloak user was created
 */
public record ProvisionTenantUserResult(UserId userId, UUID keycloakId, boolean kcCreated) {}
