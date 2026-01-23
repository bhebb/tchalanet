package com.tchalanet.server.core.tenant.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.catalog.address.application.dto.AddressDto;
import com.tchalanet.server.catalog.address.application.port.out.AddressWriterPort;
import com.tchalanet.server.catalog.address.domain.model.Address;
import com.tchalanet.server.core.tenant.application.command.model.CreateTenantCommand;
import com.tchalanet.server.core.tenant.application.port.out.TenantReaderPort;
import com.tchalanet.server.core.tenant.application.port.out.TenantWriterPort;
import com.tchalanet.server.core.tenant.domain.event.TenantCreatedEvent;
import com.tchalanet.server.core.tenant.domain.model.Tenant;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateTenantCommandHandler implements CommandHandler<CreateTenantCommand, UUID> {

  private final TenantWriterPort tenantWriterPort;
  private final TenantReaderPort tenantReaderPort;
  private final AddressWriterPort addressWriter;
  private final DomainEventPublisher publisher;
  private final Clock clock;

  @Override
  @TchTx
  public UUID handle(CreateTenantCommand cmd) {
    var codeLower = cmd.code().trim().toLowerCase();
    if (tenantReaderPort.existsByCode(codeLower)) {
      throw new IllegalArgumentException("Tenant code already exists: " + codeLower);
    }

    var tenant =
        Tenant.createDraft(
            TenantId.of(UUID.randomUUID()),
            codeLower,
            cmd.name(),
            cmd.type(),
            cmd.timezone(),
            cmd.currency());

    // handle address: either address.id provided, or address dto provided
    UUID addressId = null;
    AddressDto aDto = cmd.address();
    if (aDto != null) addressId = aDto.id();

    if (addressId == null && aDto != null) {
      var domain =
          new Address(
              null,
              aDto.line1(),
              aDto.line2(),
              aDto.city(),
              aDto.region(),
              aDto.country(),
              aDto.postalCode(),
              aDto.latitude(),
              aDto.longitude());
      addressId = addressWriter.save(domain);
    }

    if (addressId != null) {
      tenant = tenant.withAddressId(addressId);
    }

    var saved = tenantWriterPort.save(tenant);

    var evt =
        new TenantCreatedEvent(UUID.randomUUID(), Instant.now(clock), saved.id(), saved.code());
    AfterCommit.run(() -> publisher.publish(evt));

    return saved.id().value();
  }
}
