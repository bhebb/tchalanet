package com.tchalanet.server.catalog.billing.application.port.out;

import com.tchalanet.server.catalog.billing.domain.model.Plan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanReaderPort {
  Optional<Plan> findById(UUID planId);

  List<Plan> findAllPublic();
}
