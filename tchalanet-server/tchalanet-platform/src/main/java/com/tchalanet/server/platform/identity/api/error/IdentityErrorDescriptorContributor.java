package com.tchalanet.server.platform.identity.api.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Collection;
import org.springframework.stereotype.Component;

/** Registers platform identity descriptors with the application contract catalog. */
@Component
final class IdentityErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "platform.identity";
  }

  @Override
  public Collection<ErrorDescriptor> descriptors() {
    return IdentityErrorCodes.all();
  }
}
