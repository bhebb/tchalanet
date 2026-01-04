package com.tchalanet.server.core.address.application.port.out;

import com.tchalanet.server.core.address.domain.model.Address;
import java.util.UUID;

public interface AddressWriterPort {
  /** Save or update an address and return its id */
  UUID save(Address address);
}
