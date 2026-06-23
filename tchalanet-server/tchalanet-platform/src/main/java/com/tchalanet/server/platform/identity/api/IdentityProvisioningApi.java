package com.tchalanet.server.platform.identity.api;

public interface IdentityProvisioningApi {

  ProvisionedExternalUser provisionUser(ProvisionExternalUserRequest request);

  default void resetPassword(String externalSubject, String newPassword) {
    throw new UnsupportedOperationException("resetPassword not supported by this identity provider");
  }
}

