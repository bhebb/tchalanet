package com.tchalanet.server.core.tenantconfig.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.core.address.application.AddressCrudService;
import com.tchalanet.server.core.tenantconfig.application.command.model.CreateTenantCommand;
import com.tchalanet.server.core.tenantconfig.application.port.out.TenantConfigWriterPort;
import com.tchalanet.server.core.tenantconfig.domain.event.TenantCreatedEvent;
import com.tchalanet.server.core.tenantconfig.domain.model.TenantConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Command handler: Create Tenant.
 * Per DOMAIN_TENANT_CONFIG.md + command_query_handlers.md:
 * - Implements CommandHandler<CreateTenantCommand, CreateTenantResult>
 * - Create tenant in DRAFT status
 * - Optionally create/deduplicate address via core.address
 * - Publish TenantCreatedEvent after commit
 * - Translate unique constraint violation to domain error
 */
@UseCase
@RequiredArgsConstructor
public class CreateTenantCommandHandler implements VoidCommandHandler<CreateTenantCommand> {

  private final TenantConfigWriterPort writer;
  private final AddressCrudService addressCrud;
  private final DomainEventPublisher publisher;
  private final Clock clock;

  @Override
  @TchTx
  public void handle(CreateTenantCommand cmd) {
    // Create tenant (DRAFT or ACTIVE based on activate flag)
    var tenantId = TenantId.of(UUID.randomUUID());
    var tenant = TenantConfig.createDraft(
        tenantId,
        cmd.code(),
        cmd.name(),
        cmd.type(),
        cmd.timezone(),
        cmd.currency(),
        null,  // addressId - will be set later if provided
        cmd.activeThemeId()  // theme preset if provided
    );

    // Handle optional address
    if (cmd.address() != null) {
      var addressId = addressCrud.upsert(tenantId, cmd.address());
      tenant = tenant.withAddressId(addressId);
    }

    // Activate tenant if requested (default is DRAFT)
    var now = Instant.now(clock);
    if (Boolean.TRUE.equals(cmd.activate())) {
      tenant = tenant.activate(now);
    }

    // Save tenant (use create for new tenant)
    try {
      var saved = writer.create(tenant);

      // Publish event after commit
      var evt = new TenantCreatedEvent(
          UUID.randomUUID(),
          now,
          saved.id(),
          saved.code()
      );
      AfterCommit.run(() -> publisher.publish(evt));

    } catch (DataIntegrityViolationException e) {
      // Translate unique constraint violation
      throw new IllegalArgumentException("TENANT_CODE_ALREADY_EXISTS: " + tenant.code(), e);
    }
  }
}
