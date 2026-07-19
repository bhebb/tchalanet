package com.tchalanet.server.core.sellerterminal.api.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Registers seller-terminal descriptors with the application-wide error contract. */
@Component
class SellerTerminalErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "core.sellerterminal";
  }

  @Override
  public Set<ErrorDescriptor> descriptors() {
    return SellerTerminalErrorCodes.all();
  }
}
