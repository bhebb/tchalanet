package com.tchalanet.server.platform.entitlement.api.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Collection;
import org.springframework.stereotype.Component;

/** Registers platform entitlement descriptors with the application contract catalog. */
@Component
final class EntitlementErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "platform.entitlement";
  }

  @Override
  public Collection<ErrorDescriptor> descriptors() {
    return EntitlementErrorCodes.all();
  }
}
