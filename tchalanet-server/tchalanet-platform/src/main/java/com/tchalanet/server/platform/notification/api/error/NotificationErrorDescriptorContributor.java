package com.tchalanet.server.platform.notification.api.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Collection;
import org.springframework.stereotype.Component;

/** Registers platform-notification descriptors with the application contract catalog. */
@Component
final class NotificationErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "platform.notification";
  }

  @Override
  public Collection<ErrorDescriptor> descriptors() {
    return NotificationErrorCodes.all();
  }
}
