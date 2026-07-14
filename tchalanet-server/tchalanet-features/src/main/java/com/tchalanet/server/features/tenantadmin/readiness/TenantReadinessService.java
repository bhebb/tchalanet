package com.tchalanet.server.features.tenantadmin.readiness;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantReadinessService {

  private final TenantReadinessAssembler assembler;

  public TenantReadinessView current() {
    return assembler.assemble(TchContext.currentOrNull());
  }
}
