package com.tchalanet.server.core.promotion.api.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Registers promotion errors with the application-wide descriptor registry. */
@Component
class PromotionErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "core.promotion";
  }

  @Override
  public Set<ErrorDescriptor> descriptors() {
    return PromotionErrorCodes.all();
  }
}
