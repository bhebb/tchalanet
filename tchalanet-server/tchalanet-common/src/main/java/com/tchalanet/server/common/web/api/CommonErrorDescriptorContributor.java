package com.tchalanet.server.common.web.api;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Collection;
import org.springframework.stereotype.Component;

/** Registers common framework descriptors without turning common into a business-code owner. */
@Component
final class CommonErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "common.web.api";
  }

  @Override
  public Collection<ErrorDescriptor> descriptors() {
    return CommonErrorDescriptors.all();
  }
}
