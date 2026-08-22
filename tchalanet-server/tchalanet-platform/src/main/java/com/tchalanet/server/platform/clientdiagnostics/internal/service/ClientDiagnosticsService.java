package com.tchalanet.server.platform.clientdiagnostics.internal.service;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.clientdiagnostics.api.ClientDiagnosticsApi;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticBatchRequest;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticCategory;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticDebugSessionView;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticEventDetailView;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticEventRequest;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticEventView;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticIngestionContext;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticIngestionResult;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticIngestionStatus;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticPolicyRequest;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticPolicyView;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticPublicBatchRequest;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticPublicPolicyRequest;
import com.tchalanet.server.platform.clientdiagnostics.api.model.DeleteClientDiagnosticEventsResult;
import com.tchalanet.server.platform.clientdiagnostics.internal.persistence.ClientDiagnosticsJdbcRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientDiagnosticsService implements ClientDiagnosticsApi {

  private static final Duration MAX_POLICY_DURATION = Duration.ofHours(24);
  private static final int MAX_INGEST_PER_MINUTE = 60;
  private static final Set<ClientDiagnosticCategory> DEFAULT_CATEGORIES =
      Set.of(
          ClientDiagnosticCategory.API,
          ClientDiagnosticCategory.CONNECTIVITY,
          ClientDiagnosticCategory.SALE,
          ClientDiagnosticCategory.PRINT,
          ClientDiagnosticCategory.SCANNER,
          ClientDiagnosticCategory.PRINTER_CONFIG,
          ClientDiagnosticCategory.FLUTTER,
          ClientDiagnosticCategory.ASYNC,
          ClientDiagnosticCategory.DEVICE);

  private final ClientDiagnosticsJdbcRepository repository;
  private final ClientDiagnosticRedactor redactor;

  @Override
  public ClientDiagnosticIngestionResult ingest(
      ClientDiagnosticIngestionContext context, ClientDiagnosticBatchRequest request) {
    var received = request.events().size();
    var policy = getPolicy(context.tenantId(), context.sellerTerminalId());
    if (!policy.enabled()) {
      log.info(
          "client-diagnostics.ingest.disabled tenantId={} sellerTerminalId={} received={} requestId={}",
          context.tenantId(),
          context.sellerTerminalId(),
          received,
          context.requestId());
      return new ClientDiagnosticIngestionResult(
          ClientDiagnosticIngestionStatus.DISABLED,
          received,
          0,
          received,
          context.receivedAtServer());
    }

    var accepted = 0;
    var ignored = 0;
    var remainingPolicyEvents =
        Math.max(
            0,
            policy.maxEvents()
                - repository.countEventsSince(
                    context.tenantId(), context.sellerTerminalId(), policy.updatedAt()));
    var remainingRateEvents =
        Math.max(
            0,
            MAX_INGEST_PER_MINUTE
                - repository.countEventsSince(
                    context.tenantId(),
                    context.sellerTerminalId(),
                    context.receivedAtServer().minusSeconds(60)));
    var remainingEvents = Math.min(remainingPolicyEvents, remainingRateEvents);
    for (ClientDiagnosticEventRequest event : request.events()) {
      if (remainingEvents <= 0
          || !policy.categories().contains(event.category())
          || !redactor.isSafe(event)) {
        ignored++;
        continue;
      }
      accepted += repository.insertEvent(context, event);
      remainingEvents--;
    }
    ignored += received - accepted - ignored;
    return new ClientDiagnosticIngestionResult(
        ClientDiagnosticIngestionStatus.ACCEPTED,
        received,
        accepted,
        ignored,
        context.receivedAtServer());
  }

  @Override
  public ClientDiagnosticPolicyView publicPolicy(ClientDiagnosticPublicPolicyRequest request) {
    return repository
        .resolveTarget(request.tenantCode(), request.terminalCode())
        .map(target -> getPolicy(target.tenantId(), target.sellerTerminalId()))
        .orElseGet(ClientDiagnosticPolicyView::disabled);
  }

  @Override
  public ClientDiagnosticIngestionResult ingestPublic(
      String requestId, ClientDiagnosticPublicBatchRequest request) {
    var receivedAtServer = Instant.now();
    var received = request.events().size();
    return repository
        .resolveTarget(request.tenantCode(), request.terminalCode())
        .map(
            target ->
                ingest(
                    new ClientDiagnosticIngestionContext(
                        target.tenantId(), target.sellerTerminalId(), requestId, receivedAtServer),
                    new ClientDiagnosticBatchRequest(request.events())))
        .orElseGet(
            () ->
                new ClientDiagnosticIngestionResult(
                    ClientDiagnosticIngestionStatus.DISABLED,
                    received,
                    0,
                    received,
                    receivedAtServer));
  }

  @Override
  public ClientDiagnosticPolicyView getPolicy(
      TenantId tenantId, SellerTerminalId sellerTerminalId) {
    var policy = repository.getPolicy(tenantId, sellerTerminalId);
    if (!policy.enabled()) return policy;
    if (policy.expiresAt() == null || !policy.expiresAt().isAfter(Instant.now())) {
      return new ClientDiagnosticPolicyView(
          false,
          policy.expiresAt(),
          policy.maxEvents(),
          policy.categories(),
          policy.reason(),
          policy.updatedAt());
    }
    return policy;
  }

  @Override
  public ClientDiagnosticPolicyView enablePolicy(
      TenantId tenantId,
      SellerTerminalId sellerTerminalId,
      ClientDiagnosticPolicyRequest request,
      UUID actorUserId) {
    var now = Instant.now();
    var expiresAt = request.expiresAt();
    if (!expiresAt.isAfter(now) || expiresAt.isAfter(now.plus(MAX_POLICY_DURATION))) {
      throw new IllegalArgumentException(
          "Diagnostics expiry must be in the future and within 24 hours");
    }
    var categories = request.categories().isEmpty() ? DEFAULT_CATEGORIES : request.categories();
    repository.enablePolicy(
        tenantId,
        sellerTerminalId,
        expiresAt,
        request.maxEvents(),
        categories,
        request.reason(),
        actorUserId);
    return getPolicy(tenantId, sellerTerminalId);
  }

  @Override
  public ClientDiagnosticPolicyView disablePolicy(
      TenantId tenantId, SellerTerminalId sellerTerminalId, UUID actorUserId) {
    repository.disablePolicy(tenantId, sellerTerminalId, actorUserId);
    return getPolicy(tenantId, sellerTerminalId);
  }

  @Override
  public List<ClientDiagnosticEventView> recentEvents(
      TenantId tenantId, SellerTerminalId sellerTerminalId, int limit) {
    return repository.recentEvents(tenantId, sellerTerminalId, limit);
  }

  @Override
  public List<ClientDiagnosticDebugSessionView> activeDebugSessions() {
    return repository.activeDebugSessions();
  }

  @Override
  public ClientDiagnosticEventDetailView eventDetail(UUID eventId) {
    return repository.eventDetail(eventId);
  }

  @Override
  public DeleteClientDiagnosticEventsResult deleteEvents(List<UUID> eventIds) {
    var deleted = repository.deleteEvents(eventIds);
    return new DeleteClientDiagnosticEventsResult(eventIds.size(), deleted);
  }
}
