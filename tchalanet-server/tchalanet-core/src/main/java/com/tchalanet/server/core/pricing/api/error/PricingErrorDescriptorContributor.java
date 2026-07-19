package com.tchalanet.server.core.pricing.api.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Registers pricing descriptors with the application-wide error contract. */
@Component
class PricingErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "core.pricing";
  }

  @Override
  public Set<ErrorDescriptor> descriptors() {
    return PricingErrorCodes.all();
  }
}
