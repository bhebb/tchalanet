package com.tchalanet.server.platform.accesscontrol.api.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Collection;
import org.springframework.stereotype.Component;

/** Registers platform access-control descriptors with the application contract catalog. */
@Component
final class AccessControlErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "platform.accesscontrol";
  }

  @Override
  public Collection<ErrorDescriptor> descriptors() {
    return AccessControlErrorCodes.all();
  }
}
