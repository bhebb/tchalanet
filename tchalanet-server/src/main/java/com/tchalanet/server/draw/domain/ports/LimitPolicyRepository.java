package com.tchalanet.server.draw.domain.ports;

import com.tchalanet.server.draw.domain.model.LimitPolicy;
import java.util.Optional;
import java.util.UUID;

public interface LimitPolicyRepository {
  Optional<LimitPolicy> findById(UUID id);

  LimitPolicy save(LimitPolicy p);
}
