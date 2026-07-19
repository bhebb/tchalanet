package com.tchalanet.server.platform.tenant.api.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Collection;
import org.springframework.stereotype.Component;

/** Registers tenant lifecycle descriptors with the application contract catalog. */
@Component
final class TenantErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "platform.tenant";
  }

  @Override
  public Collection<ErrorDescriptor> descriptors() {
    return TenantErrorCodes.all();
  }
}
