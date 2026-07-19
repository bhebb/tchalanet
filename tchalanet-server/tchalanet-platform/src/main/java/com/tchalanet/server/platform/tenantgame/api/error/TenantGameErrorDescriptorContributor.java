package com.tchalanet.server.platform.tenantgame.api.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Collection;
import org.springframework.stereotype.Component;

/** Registers tenant-game error descriptors with the application contract catalog. */
@Component
final class TenantGameErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "platform.tenantgame";
  }

  @Override
  public Collection<ErrorDescriptor> descriptors() {
    return TenantGameErrorCodes.all();
  }
}
