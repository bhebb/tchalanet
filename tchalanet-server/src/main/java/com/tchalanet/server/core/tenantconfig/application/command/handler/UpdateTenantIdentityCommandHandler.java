package com.tchalanet.server.core.tenantconfig.application.command.handler;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.tenantconfig.application.command.model.UpdateTenantIdentityCommand;
import com.tchalanet.server.core.tenantconfig.application.port.out.TenantConfigWriterPort;
import com.tchalanet.server.core.tenantconfig.domain.event.TenantIdentityUpdatedEvent;
import com.tchalanet.server.core.tenantconfig.domain.model.TenantConfig;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateTenantIdentityCommandHandler
    implements VoidCommandHandler<UpdateTenantIdentityCommand> {

  private final TenantCatalog tenantCatalog;
  private final TenantConfigWriterPort writer;
  private final IdGenerator idGenerator;
  private final DomainEventPublisher publisher;
  private final Clock clock;

  @Override
  @TchTx
  public void handle(UpdateTenantIdentityCommand cmd) {
    var registry =
        tenantCatalog
            .findRegistryById(cmd.tenantId())
            .orElseThrow(() -> ProblemRest.notFound("tenant not found", cmd.tenantId()));

    var now = Instant.now(clock);
    TenantConfig tenant = TenantConfig.fromRegistryView(registry);
    Set<String> changedFields = new LinkedHashSet<>();

    if (cmd.name() != null && !Objects.equals(cmd.name(), tenant.name())) {
      tenant = tenant.rename(cmd.name(), now);
      changedFields.add("name");
    }

    if (cmd.timezone() != null || cmd.currency() != null) {
      ZoneId timezone = tenant.timezone();
      Currency currency = tenant.currency();

      if (cmd.timezone() != null) {
        try {
          timezone = ZoneId.of(cmd.timezone().trim());
        } catch (RuntimeException e) {
          throw ProblemRest.badRequest("invalid timezone: " + cmd.timezone());
        }
      }

      if (cmd.currency() != null) {
        try {
          currency = Currency.getInstance(cmd.currency().trim().toUpperCase());
        } catch (RuntimeException e) {
          throw ProblemRest.badRequest("invalid currency: " + cmd.currency());
        }
      }

      if (!Objects.equals(timezone, tenant.timezone()) || !Objects.equals(currency, tenant.currency())) {
        if (!Objects.equals(timezone, tenant.timezone())) {
          changedFields.add("timezone");
        }
        if (!Objects.equals(currency, tenant.currency())) {
          changedFields.add("currency");
        }
        tenant = tenant.updateLocale(timezone, currency, now);
      }
    }

    if (changedFields.isEmpty()) {
      return;
    }

    writer.update(tenant);
    var event =
        new TenantIdentityUpdatedEvent(
            EventId.of(idGenerator.newUuid()), now, tenant.id(), Set.copyOf(changedFields));
    AfterCommit.run(() -> publisher.publish(event));
  }
}
