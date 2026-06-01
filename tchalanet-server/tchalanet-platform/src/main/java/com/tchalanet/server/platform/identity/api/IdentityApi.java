package com.tchalanet.server.platform.identity.api;

import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.identity.api.model.request.BootstrapCurrentUserRequest;
import com.tchalanet.server.platform.identity.api.model.request.GetCurrentUserRequest;
import com.tchalanet.server.platform.identity.api.model.request.GetUserProfileRequest;
import com.tchalanet.server.platform.identity.api.model.result.BootstrapUserResult;
import com.tchalanet.server.platform.identity.api.model.result.CreateUserResult;
import com.tchalanet.server.platform.identity.api.model.view.AppUserView;
import com.tchalanet.server.platform.identity.api.model.view.CurrentUserView;
import com.tchalanet.server.platform.identity.api.model.view.UserProfileView;
import java.util.Optional;
import java.util.UUID;

public interface IdentityApi {

    CurrentUserView getCurrentUser(GetCurrentUserRequest request);

    BootstrapUserResult bootstrapCurrentUser(BootstrapCurrentUserRequest request);

    UserProfileView getUserProfile(GetUserProfileRequest request);

    Optional<AppUserView> findAppUser(UUID userId);

    long countTenantUsers();

    /**
     * Creates a user and assigns them to a tenant with the given role.
     * Reuses the same service logic as POST /admin/identity/users.
     * Caller must run within the target tenant's RLS context.
     */
    CreateUserResult createTenantUser(
        TenantId tenantId,
        String email,
        String firstName,
        String lastName,
        TchRole role);
}
