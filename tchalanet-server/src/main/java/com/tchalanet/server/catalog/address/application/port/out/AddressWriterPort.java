package com.tchalanet.server.catalog.address.application.port.out;

import com.tchalanet.server.catalog.address.domain.model.Address;
import java.util.UUID;

public interface AddressWriterPort {
  /** Save or update an address and return its id */
  UUID save(Address address);
}
