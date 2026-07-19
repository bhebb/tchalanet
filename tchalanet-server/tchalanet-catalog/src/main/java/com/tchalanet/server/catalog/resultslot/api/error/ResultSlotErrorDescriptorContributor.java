package com.tchalanet.server.catalog.resultslot.api.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Registers result-slot errors with the application-wide descriptor registry. */
@Component
class ResultSlotErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "catalog.resultslot";
  }

  @Override
  public Set<ErrorDescriptor> descriptors() {
    return ResultSlotErrorCodes.all();
  }
}
