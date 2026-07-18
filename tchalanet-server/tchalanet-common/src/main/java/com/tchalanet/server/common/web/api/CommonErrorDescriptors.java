package com.tchalanet.server.common.web.api;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Shared descriptors for framework-owned authentication and authorization failures. */
@UtilityClass
public class CommonErrorDescriptors {

  private static final Set<ErrorAudience> PRIVATE_AUDIENCES =
      Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.WEB_PLATFORM, ErrorAudience.MOBILE);

  public static final ErrorDescriptor AUTHENTICATION_REQUIRED =
      new ErrorDescriptor(
          CommonErrorCodes.AUTHENTICATION_REQUIRED,
          ErrorCategory.AUTHENTICATION,
          HttpStatus.UNAUTHORIZED,
          ErrorRetryPolicy.AFTER_REAUTH,
          PRIVATE_AUDIENCES,
          Set.of());

  public static final ErrorDescriptor ACCESS_DENIED =
      new ErrorDescriptor(
          CommonErrorCodes.ACCESS_DENIED,
          ErrorCategory.AUTHORIZATION,
          HttpStatus.FORBIDDEN,
          ErrorRetryPolicy.NEVER,
          PRIVATE_AUDIENCES,
          Set.of());

  public static Set<ErrorDescriptor> all() {
    return Set.of(AUTHENTICATION_REQUIRED, ACCESS_DENIED);
  }
}
