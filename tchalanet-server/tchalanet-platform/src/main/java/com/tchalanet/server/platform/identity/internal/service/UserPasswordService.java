package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.IdentityProvisioningApi;
import com.tchalanet.server.platform.identity.internal.persistence.adapter.AppUserJpaAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPasswordService {

  private final AppUserJpaAdapter users;
  private final IdentityProvisioningApi identityProvisioning;

  public void resetPasswordByEmail(String email, String newPassword) {
    var user = users.findByEmailOrPhone(email, null)
        .orElseThrow(() -> ProblemRest.notFound("No account found for this email"));
    var externalSubject = users.findExternalSubject(user.id(), IdentityProviderType.FIREBASE)
        .orElseThrow(() -> ProblemRest.unprocessable("No Firebase identity linked for this account"));
    identityProvisioning.resetPassword(externalSubject, newPassword);
  }
}
