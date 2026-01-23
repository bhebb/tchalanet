package com.tchalanet.server.catalog.address.application.port.out;

import com.tchalanet.server.catalog.address.domain.model.Address;
import java.util.Optional;
import java.util.UUID;

public interface AddressReaderPort {
  Optional<Address> findById(UUID id);
}
