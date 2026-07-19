package com.tchalanet.server.core.sales.api.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Registers the sale error contract with the application-wide descriptor registry. */
@Component
class SalesErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "core.sales";
  }

  @Override
  public Set<ErrorDescriptor> descriptors() {
    return SalesErrorCodes.all();
  }
}
