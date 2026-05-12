package com.tchalanet.server.core.tenantconfig.application.command.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.catalog.tenant.api.model.TenantBootstrapView;
import com.tchalanet.server.catalog.tenant.api.model.TenantRegistryView;
import com.tchalanet.server.catalog.tenant.api.model.TenantStatsView;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.types.enums.TenantStatus;
import com.tchalanet.server.common.types.enums.TenantType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.tenantconfig.application.command.model.UpdateTenantIdentityCommand;
import com.tchalanet.server.core.tenantconfig.application.port.out.TenantConfigWriterPort;
import com.tchalanet.server.core.tenantconfig.domain.event.TenantIdentityUpdatedEvent;
import com.tchalanet.server.core.tenantconfig.domain.model.TenantConfig;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class UpdateTenantIdentityCommandHandlerTest {

  private static final TenantId TENANT_ID = TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000111"));
  private static final Instant NOW = Instant.parse("2026-04-28T15:00:00Z");
  private static final UUID EVENT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000999");

  @Test
  void updatesNameOnly() {
    var writer = new RecordingWriter();
    var publisher = new RecordingPublisher();
    var handler = newHandler(registry("Acme", "America/New_York", "USD"), writer, publisher);

    handler.handle(new UpdateTenantIdentityCommand(TENANT_ID, "Acme Plus", null, null));

    assertNotNull(writer.updated);
    assertEquals("Acme Plus", writer.updated.name());
    var event = assertInstanceOf(TenantIdentityUpdatedEvent.class, publisher.event);
    assertEquals(Set.of("name"), event.changedFields());
    assertEquals(EVENT_UUID, event.eventId().value());
  }

  @Test
  void updatesLocaleOnly() {
    var writer = new RecordingWriter();
    var publisher = new RecordingPublisher();
    var handler = newHandler(registry("Acme", "America/New_York", "USD"), writer, publisher);

    handler.handle(new UpdateTenantIdentityCommand(TENANT_ID, null, "America/Port-au-Prince", "HTG"));

    assertEquals(ZoneId.of("America/Port-au-Prince"), writer.updated.timezone());
    assertEquals(Currency.getInstance("HTG"), writer.updated.currency());
    var event = assertInstanceOf(TenantIdentityUpdatedEvent.class, publisher.event);
    assertEquals(Set.of("timezone", "currency"), event.changedFields());
  }

  @Test
  void updatesNameAndLocale() {
    var writer = new RecordingWriter();
    var publisher = new RecordingPublisher();
    var handler = newHandler(registry("Acme", "America/New_York", "USD"), writer, publisher);

    handler.handle(
        new UpdateTenantIdentityCommand(TENANT_ID, "Acme Haiti", "America/Port-au-Prince", "HTG"));

    assertEquals("Acme Haiti", writer.updated.name());
    assertEquals(ZoneId.of("America/Port-au-Prince"), writer.updated.timezone());
    assertEquals(Currency.getInstance("HTG"), writer.updated.currency());
    var event = assertInstanceOf(TenantIdentityUpdatedEvent.class, publisher.event);
    assertEquals(Set.of("name", "timezone", "currency"), event.changedFields());
  }

  @Test
  void throwsNotFoundWhenTenantMissing() {
    var writer = new RecordingWriter();
    var publisher = new RecordingPublisher();
    var handler =
        new UpdateTenantIdentityCommandHandler(
            new MissingTenantCatalog(),
            writer,
            () -> EVENT_UUID,
            publisher,
            Clock.fixed(NOW, ZoneId.of("UTC")));

    var ex =
        assertThrows(
            ProblemRestException.class,
            () -> handler.handle(new UpdateTenantIdentityCommand(TENANT_ID, "Acme", null, null)));

    assertEquals("tenant not found: " + TENANT_ID, ex.getProblem().getDetail());
  }

  @Test
  void throwsBadRequestForInvalidTimezone() {
    var writer = new RecordingWriter();
    var publisher = new RecordingPublisher();
    var handler = newHandler(registry("Acme", "America/New_York", "USD"), writer, publisher);

    var ex =
        assertThrows(
            ProblemRestException.class,
            () -> handler.handle(new UpdateTenantIdentityCommand(TENANT_ID, null, "Mars/Phobos", null)));

    assertEquals("invalid timezone: Mars/Phobos", ex.getProblem().getDetail());
  }

  @Test
  void noDeactivateEndpointMethodRemains() {
    boolean present =
        java.util.Arrays.stream(
                com.tchalanet.server.core.tenantconfig.infra.web.TenantAdminController.class
                    .getDeclaredMethods())
            .anyMatch(method -> method.getName().equals("deactivate"));

    assertTrue(!present);
  }

  private static UpdateTenantIdentityCommandHandler newHandler(
      TenantRegistryView registry, RecordingWriter writer, RecordingPublisher publisher) {
    return new UpdateTenantIdentityCommandHandler(
        new SingleTenantCatalog(registry),
        writer,
        () -> EVENT_UUID,
        publisher,
        Clock.fixed(NOW, ZoneId.of("UTC")));
  }

  private static TenantRegistryView registry(String name, String timezone, String currency) {
    return new TenantRegistryView(
        TENANT_ID,
        "acme",
        name,
        TenantStatus.ACTIVE,
        TenantType.BORLETTE,
        ZoneId.of(timezone),
        Currency.getInstance(currency),
        Optional.empty(),
        Optional.empty());
  }

  private static final class RecordingWriter implements TenantConfigWriterPort {
    private TenantConfig updated;

    @Override
    public TenantConfig create(TenantConfig tenant) {
      throw new UnsupportedOperationException();
    }

    @Override
    public TenantConfig update(TenantConfig tenant) {
      this.updated = tenant;
      return tenant;
    }
  }

  private static final class RecordingPublisher implements DomainEventPublisher {
    private DomainEvent event;

    @Override
    public void publish(DomainEvent event) {
      this.event = event;
    }

    @Override
    public void publish(Collection<? extends DomainEvent> events) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class SingleTenantCatalog implements TenantCatalog {
    private final TenantRegistryView registry;

    private SingleTenantCatalog(TenantRegistryView registry) {
      this.registry = registry;
    }

    @Override
    public Optional<TenantRegistryView> findRegistryById(TenantId tenantId) {
      return Optional.of(registry);
    }

    @Override
    public Optional<TenantId> findIdByCode(String codeLower) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TenantBootstrapView> findBootstrapByCode(String codeLower) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TenantBootstrapView> findBootstrapById(TenantId tenantId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TenantRegistryView> findRegistryByCode(String codeLower) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<TenantId> listActiveTenantIds() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Page<TenantRegistryView> listTenants(Pageable pageable) {
      throw new UnsupportedOperationException();
    }

    @Override
    public TenantStatsView stats() {
      throw new UnsupportedOperationException();
    }
  }

  private static final class MissingTenantCatalog implements TenantCatalog {
    @Override
    public Optional<TenantRegistryView> findRegistryById(TenantId tenantId) {
      return Optional.empty();
    }

    @Override
    public Optional<TenantId> findIdByCode(String codeLower) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TenantBootstrapView> findBootstrapByCode(String codeLower) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TenantBootstrapView> findBootstrapById(TenantId tenantId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TenantRegistryView> findRegistryByCode(String codeLower) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<TenantId> listActiveTenantIds() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Page<TenantRegistryView> listTenants(Pageable pageable) {
      throw new UnsupportedOperationException();
    }

    @Override
    public TenantStatsView stats() {
      throw new UnsupportedOperationException();
    }
  }
}
