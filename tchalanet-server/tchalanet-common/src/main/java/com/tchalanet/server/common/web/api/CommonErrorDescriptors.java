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

  private static final Set<ErrorAudience> ALL_AUDIENCES =
      Set.of(
          ErrorAudience.WEB_PUBLIC,
          ErrorAudience.WEB_ADMIN,
          ErrorAudience.WEB_PLATFORM,
          ErrorAudience.MOBILE);

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

  public static final ErrorDescriptor RESOURCE_NOT_FOUND =
      new ErrorDescriptor(
          CommonErrorCodes.RESOURCE_NOT_FOUND,
          ErrorCategory.NOT_FOUND,
          HttpStatus.NOT_FOUND,
          ErrorRetryPolicy.NEVER,
          ALL_AUDIENCES,
          Set.of());

  public static final ErrorDescriptor VALIDATION_FAILED =
      new ErrorDescriptor(
          CommonErrorCodes.VALIDATION_FAILED,
          ErrorCategory.VALIDATION,
          HttpStatus.BAD_REQUEST,
          ErrorRetryPolicy.AFTER_USER_ACTION,
          ALL_AUDIENCES,
          Set.of());

  public static final ErrorDescriptor VALIDATION_CONSTRAINT_VIOLATION =
      new ErrorDescriptor(
          CommonErrorCodes.VALIDATION_CONSTRAINT_VIOLATION,
          ErrorCategory.VALIDATION,
          HttpStatus.BAD_REQUEST,
          ErrorRetryPolicy.AFTER_USER_ACTION,
          ALL_AUDIENCES,
          Set.of());

  public static final ErrorDescriptor REQUEST_NOT_READABLE =
      new ErrorDescriptor(
          CommonErrorCodes.REQUEST_NOT_READABLE,
          ErrorCategory.VALIDATION,
          HttpStatus.BAD_REQUEST,
          ErrorRetryPolicy.AFTER_USER_ACTION,
          ALL_AUDIENCES,
          Set.of());

  public static final ErrorDescriptor REQUEST_MISSING_PARAMETER =
      new ErrorDescriptor(
          CommonErrorCodes.REQUEST_MISSING_PARAMETER,
          ErrorCategory.VALIDATION,
          HttpStatus.BAD_REQUEST,
          ErrorRetryPolicy.AFTER_USER_ACTION,
          ALL_AUDIENCES,
          Set.of());

  public static final ErrorDescriptor REQUEST_TYPE_MISMATCH =
      new ErrorDescriptor(
          CommonErrorCodes.REQUEST_TYPE_MISMATCH,
          ErrorCategory.VALIDATION,
          HttpStatus.BAD_REQUEST,
          ErrorRetryPolicy.AFTER_USER_ACTION,
          ALL_AUDIENCES,
          Set.of());

  public static final ErrorDescriptor INTERNAL_UNEXPECTED =
      new ErrorDescriptor(
          CommonErrorCodes.INTERNAL_UNEXPECTED,
          ErrorCategory.UNEXPECTED,
          HttpStatus.INTERNAL_SERVER_ERROR,
          ErrorRetryPolicy.AFTER_DELAY,
          ALL_AUDIENCES,
          Set.of());

  public static Set<ErrorDescriptor> all() {
    return Set.of(
        AUTHENTICATION_REQUIRED,
        ACCESS_DENIED,
        RESOURCE_NOT_FOUND,
        VALIDATION_FAILED,
        VALIDATION_CONSTRAINT_VIOLATION,
        REQUEST_NOT_READABLE,
        REQUEST_MISSING_PARAMETER,
        REQUEST_TYPE_MISMATCH,
        INTERNAL_UNEXPECTED);
  }
}
