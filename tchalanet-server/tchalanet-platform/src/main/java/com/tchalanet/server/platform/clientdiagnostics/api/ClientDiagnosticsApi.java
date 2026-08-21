package com.tchalanet.server.platform.clientdiagnostics.api;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticBatchRequest;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticEventDetailView;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticEventView;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticIngestionContext;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticIngestionResult;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticPolicyRequest;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticPolicyView;
import java.util.List;
import java.util.UUID;

public interface ClientDiagnosticsApi {

  ClientDiagnosticIngestionResult ingest(
      ClientDiagnosticIngestionContext context, ClientDiagnosticBatchRequest request);

  ClientDiagnosticPolicyView getPolicy(TenantId tenantId, SellerTerminalId sellerTerminalId);

  ClientDiagnosticPolicyView enablePolicy(
      TenantId tenantId,
      SellerTerminalId sellerTerminalId,
      ClientDiagnosticPolicyRequest request,
      UUID actorUserId);

  ClientDiagnosticPolicyView disablePolicy(
      TenantId tenantId, SellerTerminalId sellerTerminalId, UUID actorUserId);

  List<ClientDiagnosticEventView> recentEvents(
      TenantId tenantId, SellerTerminalId sellerTerminalId, int limit);

  ClientDiagnosticEventDetailView eventDetail(UUID eventId);
}
