package com.tchalanet.server.features.pos.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Collection;
import org.springframework.stereotype.Component;

/** Registers POS feature error descriptors with the application contract catalog. */
@Component
final class PosErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "features.pos";
  }

  @Override
  public Collection<ErrorDescriptor> descriptors() {
    return PosErrorCodes.all();
  }
}
