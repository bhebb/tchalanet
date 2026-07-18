package com.tchalanet.server.common.context;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Collection;
import org.springframework.stereotype.Component;

/** Registers the request-context failures shared by authenticated application surfaces. */
@Component
final class RequestContextErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "common.context";
  }

  @Override
  public Collection<ErrorDescriptor> descriptors() {
    return RequestContextErrorCodes.all();
  }
}
