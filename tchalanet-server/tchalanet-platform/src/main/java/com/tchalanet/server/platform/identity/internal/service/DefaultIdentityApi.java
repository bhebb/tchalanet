package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.platform.identity.api.IdentityApi;
import com.tchalanet.server.platform.identity.api.model.request.BootstrapCurrentUserRequest;
import com.tchalanet.server.platform.identity.api.model.request.GetCurrentUserRequest;
import com.tchalanet.server.platform.identity.api.model.request.GetUserProfileRequest;
import com.tchalanet.server.platform.identity.api.model.result.BootstrapUserResult;
import com.tchalanet.server.platform.identity.api.model.view.CurrentUserView;
import com.tchalanet.server.platform.identity.api.model.view.UserProfileView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultIdentityApi implements IdentityApi {

  private final CurrentUserProfileService profiles;
  private final UserBootstrapService bootstrapService;

  @Override
  public CurrentUserView getCurrentUser(GetCurrentUserRequest request) {
    return profiles.getCurrentUser(request.userId());
  }

  @Override
  public BootstrapUserResult bootstrapCurrentUser(BootstrapCurrentUserRequest request) {
    return bootstrapService.bootstrap(request);
  }

  @Override
  public UserProfileView getUserProfile(GetUserProfileRequest request) {
    return profiles.getUserProfile(request.userId());
  }
}
