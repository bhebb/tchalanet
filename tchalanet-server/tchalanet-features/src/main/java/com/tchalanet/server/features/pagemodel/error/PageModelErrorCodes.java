package com.tchalanet.server.features.pagemodel.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing failures and degradations owned by the PageModel feature. */
@UtilityClass
public class PageModelErrorCodes {

  private static final Set<ErrorAudience> PRIVATE_AUDIENCES =
      Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.WEB_PLATFORM, ErrorAudience.MOBILE);
  private static final Set<ErrorAudience> ALL_AUDIENCES =
      Set.of(
          ErrorAudience.WEB_PUBLIC,
          ErrorAudience.WEB_ADMIN,
          ErrorAudience.WEB_PLATFORM,
          ErrorAudience.MOBILE);

  public static final ErrorDescriptor ACCESS_DENIED = authorization("pagemodel.access_denied");
  public static final ErrorDescriptor TENANT_OVERRIDE_FORBIDDEN =
      authorization("pagemodel.tenant_override_forbidden");
  public static final ErrorDescriptor NOT_FOUND = notFound("pagemodel.not_found", ALL_AUDIENCES);
  public static final ErrorDescriptor WIDGET_UNAVAILABLE =
      unavailable("pagemodel.widget.unavailable");
  public static final ErrorDescriptor WIDGET_NO_PROVIDER =
      unavailable("pagemodel.widget.no_provider");
  public static final ErrorDescriptor WIDGET_ROLE_DENIED =
      authorization("pagemodel.widget.role_denied");
  public static final ErrorDescriptor FRAGMENT_KEY_REQUIRED =
      validation("pagemodel.fragment.key_required");
  public static final ErrorDescriptor FRAGMENT_NOT_FOUND =
      notFound("pagemodel.fragment.not_found", ALL_AUDIENCES);
  public static final ErrorDescriptor FRAGMENT_INVALID = unavailable("pagemodel.fragment.invalid");

  public static Set<ErrorDescriptor> all() {
    return Set.of(
        ACCESS_DENIED,
        TENANT_OVERRIDE_FORBIDDEN,
        NOT_FOUND,
        WIDGET_UNAVAILABLE,
        WIDGET_NO_PROVIDER,
        WIDGET_ROLE_DENIED,
        FRAGMENT_KEY_REQUIRED,
        FRAGMENT_NOT_FOUND,
        FRAGMENT_INVALID);
  }

  private static ErrorDescriptor authorization(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.AUTHORIZATION,
        HttpStatus.FORBIDDEN,
        ErrorRetryPolicy.AFTER_REAUTH,
        PRIVATE_AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor validation(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.VALIDATION,
        HttpStatus.BAD_REQUEST,
        ErrorRetryPolicy.AFTER_USER_ACTION,
        ALL_AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor notFound(String code, Set<ErrorAudience> audiences) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.NOT_FOUND,
        HttpStatus.NOT_FOUND,
        ErrorRetryPolicy.AFTER_USER_ACTION,
        audiences,
        Set.of());
  }

  private static ErrorDescriptor unavailable(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.SERVICE_UNAVAILABLE,
        HttpStatus.SERVICE_UNAVAILABLE,
        ErrorRetryPolicy.RETRY_SAME_INTENT,
        ALL_AUDIENCES,
        Set.of());
  }
}
