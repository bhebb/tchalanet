package com.tchalanet.server.features.pagemodel.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Collection;
import org.springframework.stereotype.Component;

/** Registers PageModel feature error descriptors with the application contract catalog. */
@Component
final class PageModelErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "features.pagemodel";
  }

  @Override
  public Collection<ErrorDescriptor> descriptors() {
    return PageModelErrorCodes.all();
  }
}
