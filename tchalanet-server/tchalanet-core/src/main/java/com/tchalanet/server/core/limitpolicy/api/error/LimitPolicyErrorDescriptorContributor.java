package com.tchalanet.server.core.limitpolicy.api.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Registers limit policy errors with the application-wide descriptor registry. */
@Component
class LimitPolicyErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "core.limitpolicy";
  }

  @Override
  public Set<ErrorDescriptor> descriptors() {
    return LimitPolicyErrorCodes.all();
  }
}
