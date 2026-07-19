package com.tchalanet.server.platform.publiccontent.api.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Collection;
import org.springframework.stereotype.Component;

/** Registers platform-public-content descriptors with the application contract catalog. */
@Component
final class PublicContentErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "platform.publiccontent";
  }

  @Override
  public Collection<ErrorDescriptor> descriptors() {
    return PublicContentErrorCodes.all();
  }
}
