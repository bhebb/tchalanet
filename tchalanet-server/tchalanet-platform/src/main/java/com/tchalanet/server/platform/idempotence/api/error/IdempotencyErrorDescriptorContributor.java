package com.tchalanet.server.platform.idempotence.api.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Collection;
import org.springframework.stereotype.Component;

@Component
final class IdempotencyErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "platform.idempotence";
  }

  @Override
  public Collection<ErrorDescriptor> descriptors() {
    return IdempotencyErrorCodes.all();
  }
}
