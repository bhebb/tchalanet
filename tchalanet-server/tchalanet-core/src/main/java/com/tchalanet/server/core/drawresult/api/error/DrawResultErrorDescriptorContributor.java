package com.tchalanet.server.core.drawresult.api.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Registers draw-result errors with the application-wide descriptor registry. */
@Component
class DrawResultErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "core.drawresult";
  }

  @Override
  public Set<ErrorDescriptor> descriptors() {
    return DrawResultErrorCodes.all();
  }
}
