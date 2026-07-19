package com.tchalanet.server.platform.publiccontent.api.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing failures owned by platform public content. */
@UtilityClass
public class PublicContentErrorCodes {

  public static final ErrorDescriptor ITEM_NOT_FOUND =
      new ErrorDescriptor(
          "publiccontent.item.not_found",
          ErrorCategory.NOT_FOUND,
          HttpStatus.NOT_FOUND,
          ErrorRetryPolicy.NEVER,
          Set.of(ErrorAudience.WEB_PLATFORM),
          Set.of());

  public static Set<ErrorDescriptor> all() {
    return Set.of(ITEM_NOT_FOUND);
  }
}
