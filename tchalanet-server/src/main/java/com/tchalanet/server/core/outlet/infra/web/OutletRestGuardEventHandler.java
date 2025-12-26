package com.tchalanet.server.core.outlet.infra.web;

import com.tchalanet.server.core.outlet.infra.persistence.OutletEntity;
import com.tchalanet.server.core.outlet.infra.persistence.OutletSpringRepository;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler(OutletEntity.class)
public class OutletRestGuardEventHandler {

  private final OutletSpringRepository repo;

  public OutletRestGuardEventHandler(OutletSpringRepository repo) {
    this.repo = repo;
  }

  @HandleBeforeSave
  public void beforeSave(OutletEntity incoming) {
    var existing =
        repo.findById(incoming.getId())
            .orElseThrow(() -> new IllegalArgumentException("Outlet not found"));

    if (incoming.isSalesBlocked() != existing.isSalesBlocked()
        || !eq(incoming.getSalesBlockReason(), existing.getSalesBlockReason())
        || !eq(incoming.getTimezone(), existing.getTimezone())
        || !eq(incoming.getBusinessDayCutoff(), existing.getBusinessDayCutoff())
        || incoming.isReceiptPrintingEnabled() != existing.isReceiptPrintingEnabled()
        || !eq(incoming.getReceiptHeaderMessage(), existing.getReceiptHeaderMessage())
        || !eq(incoming.getReceiptFooterMessage(), existing.getReceiptFooterMessage())
        || incoming.isRequireOpeningFloat() != existing.isRequireOpeningFloat()) {
      throw new IllegalStateException(
          "Outlet config fields must be updated via /admin/outlets/{id}/config");
    }
  }

  private static boolean eq(Object a, Object b) {
    return a == null ? b == null : a.equals(b);
  }
}
