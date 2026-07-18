package com.tchalanet.server.platform.identity.api.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing failures owned by platform identity. */
@UtilityClass
public class IdentityErrorCodes {

  private static final Set<ErrorAudience> LOGIN_AUDIENCES =
      Set.of(ErrorAudience.WEB_PUBLIC, ErrorAudience.WEB_ADMIN, ErrorAudience.WEB_PLATFORM);

  /**
   * Deliberately generic public-login failure.
   *
   * <p>Username lookup must not reveal whether a username exists, is active, or has a provider
   * identity. The client therefore receives one stable code for every rejected lookup.
   */
  public static final ErrorDescriptor AUTH_INVALID_CREDENTIALS =
      new ErrorDescriptor(
          "identity.auth.invalid_credentials",
          ErrorCategory.AUTHENTICATION,
          HttpStatus.FORBIDDEN,
          ErrorRetryPolicy.AFTER_USER_ACTION,
          LOGIN_AUDIENCES,
          Set.of());

  public static Set<ErrorDescriptor> all() {
    return Set.of(AUTH_INVALID_CREDENTIALS);
  }
}
