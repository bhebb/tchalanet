package com.tchalanet.server.platform.identity.api;

/**
 * The external identity provider has no user for the given subject. Callers holding enough context
 * (email, display name) may recover by re-provisioning the account under the same subject.
 */
public class IdentityUserNotFoundException extends IllegalStateException {

  public IdentityUserNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
