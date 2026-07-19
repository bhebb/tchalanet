package com.tchalanet.server.catalog.drawchannel.api.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Registers draw-channel errors with the application-wide descriptor registry. */
@Component
class DrawChannelErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "catalog.drawchannel";
  }

  @Override
  public Set<ErrorDescriptor> descriptors() {
    return DrawChannelErrorCodes.all();
  }
}
