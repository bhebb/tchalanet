package com.tchalanet.server.features.tenantadmin.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Collection;
import org.springframework.stereotype.Component;

/** Registers tenant-admin feature error descriptors with the application contract catalog. */
@Component
final class TenantAdminErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "features.tenantadmin";
  }

  @Override
  public Collection<ErrorDescriptor> descriptors() {
    return TenantAdminErrorCodes.all();
  }
}
