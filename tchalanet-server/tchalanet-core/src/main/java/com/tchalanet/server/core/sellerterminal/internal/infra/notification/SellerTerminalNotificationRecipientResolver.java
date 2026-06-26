package com.tchalanet.server.core.sellerterminal.internal.infra.notification;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;
import com.tchalanet.server.core.sellerterminal.internal.infra.persistence.SellerTerminalJpaEntity;
import com.tchalanet.server.core.sellerterminal.internal.infra.persistence.SellerTerminalJpaRepository;
import com.tchalanet.server.platform.notification.api.NotificationRecipientResolver;
import com.tchalanet.server.platform.notification.api.model.NotificationActorType;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationTarget;
import com.tchalanet.server.platform.notification.api.model.view.NotificationRecipientContact;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SellerTerminalNotificationRecipientResolver implements NotificationRecipientResolver {

  private final SellerTerminalJpaRepository terminals;

  @Override
  public boolean supportsAudience(NotificationAudienceType audienceType) {
    return audienceType == NotificationAudienceType.TENANT_SELLER_TERMINALS;
  }

  @Override
  public boolean supportsTarget(NotificationActorType actorType) {
    return actorType == NotificationActorType.SELLER_TERMINAL;
  }

  @Override
  public List<NotificationRecipientContact> resolveAudience(
      TenantId tenantId, NotificationAudienceType audienceType) {
    if (tenantId == null || audienceType != NotificationAudienceType.TENANT_SELLER_TERMINALS) {
      return List.of();
    }
    return terminals
        .findByTenantIdAndStatusAndDeletedAtIsNull(tenantId.value(), SellerTerminalStatus.ACTIVE)
        .stream()
        .map(this::toContact)
        .toList();
  }

  @Override
  public List<NotificationRecipientContact> resolveTargets(
      TenantId tenantId, Set<NotificationTarget> targets) {
    if (tenantId == null || targets == null || targets.isEmpty()) {
      return List.of();
    }
    var terminalIds =
        targets.stream()
            .filter(target -> target.actorType() == NotificationActorType.SELLER_TERMINAL)
            .map(NotificationTarget::actorId)
            .toList();
    if (terminalIds.isEmpty()) {
      return List.of();
    }
    return terminals
        .findByTenantIdAndIdInAndDeletedAtIsNull(tenantId.value(), terminalIds)
        .stream()
        .map(this::toContact)
        .toList();
  }

  private NotificationRecipientContact toContact(SellerTerminalJpaEntity terminal) {
    return new NotificationRecipientContact(
        TenantId.of(terminal.getTenantId()),
        NotificationTarget.sellerTerminal(terminal.getId()),
        null,
        terminal.getEmail(),
        terminal.getPhoneNumber());
  }
}
