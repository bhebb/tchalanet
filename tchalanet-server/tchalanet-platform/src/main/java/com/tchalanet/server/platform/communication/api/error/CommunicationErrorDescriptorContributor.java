package com.tchalanet.server.platform.communication.api.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Collection;
import org.springframework.stereotype.Component;

/** Registers outbound-communication descriptors with the application contract catalog. */
@Component
final class CommunicationErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "platform.communication";
  }

  @Override
  public Collection<ErrorDescriptor> descriptors() {
    return CommunicationErrorCodes.all();
  }
}
