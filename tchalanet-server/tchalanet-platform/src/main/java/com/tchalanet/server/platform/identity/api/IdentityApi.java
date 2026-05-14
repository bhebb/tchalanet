package com.tchalanet.server.platform.identity.api;

import com.tchalanet.server.platform.identity.api.model.request.BootstrapCurrentUserRequest;
import com.tchalanet.server.platform.identity.api.model.request.GetCurrentUserRequest;
import com.tchalanet.server.platform.identity.api.model.request.GetUserProfileRequest;
import com.tchalanet.server.platform.identity.api.model.result.BootstrapUserResult;
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
}
