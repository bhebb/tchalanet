package com.tchalanet.server.features.reporting.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Collection;
import org.springframework.stereotype.Component;

@Component
class ReportingErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "features.reporting";
  }

  @Override
  public Collection<ErrorDescriptor> descriptors() {
    return ReportingErrorCodes.all();
  }
}
