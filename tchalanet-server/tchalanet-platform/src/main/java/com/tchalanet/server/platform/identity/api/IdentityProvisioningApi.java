package com.tchalanet.server.platform.identity.api;

public interface IdentityProvisioningApi {

  ProvisionedExternalUser provisionUser(ProvisionExternalUserRequest request);
}

