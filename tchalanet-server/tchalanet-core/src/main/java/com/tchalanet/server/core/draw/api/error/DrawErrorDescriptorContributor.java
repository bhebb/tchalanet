package com.tchalanet.server.core.draw.api.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Registers draw errors with the application-wide descriptor registry. */
@Component
class DrawErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "core.draw";
  }

  @Override
  public Set<ErrorDescriptor> descriptors() {
    return DrawErrorCodes.all();
  }
}
