package com.tchalanet.server.features.ops.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Collection;
import org.springframework.stereotype.Component;

/** Registers platform-ops descriptors with the application contract catalog. */
@Component
final class OpsErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "features.ops";
  }

  @Override
  public Collection<ErrorDescriptor> descriptors() {
    return OpsErrorCodes.all();
  }
}
