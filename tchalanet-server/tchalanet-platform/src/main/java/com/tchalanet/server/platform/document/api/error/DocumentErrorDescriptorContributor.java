package com.tchalanet.server.platform.document.api.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Collection;
import org.springframework.stereotype.Component;

/** Registers document-rendering descriptors with the application contract catalog. */
@Component
final class DocumentErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "platform.document";
  }

  @Override
  public Collection<ErrorDescriptor> descriptors() {
    return DocumentErrorCodes.all();
  }
}
