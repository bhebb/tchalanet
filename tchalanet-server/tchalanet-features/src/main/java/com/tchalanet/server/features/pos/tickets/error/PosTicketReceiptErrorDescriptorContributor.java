package com.tchalanet.server.features.pos.tickets.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Collection;
import org.springframework.stereotype.Component;

/** Registers POS ticket receipt error descriptors with the application contract catalog. */
@Component
final class PosTicketReceiptErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "features.pos.tickets";
  }

  @Override
  public Collection<ErrorDescriptor> descriptors() {
    return PosTicketReceiptErrorCodes.all();
  }
}
