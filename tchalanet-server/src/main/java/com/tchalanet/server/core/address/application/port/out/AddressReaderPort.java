package com.tchalanet.server.core.address.application.port.out;

import com.tchalanet.server.core.address.domain.model.Address;
import java.util.Optional;
import java.util.UUID;

public interface AddressReaderPort {
  Optional<Address> findById(UUID id);
}
