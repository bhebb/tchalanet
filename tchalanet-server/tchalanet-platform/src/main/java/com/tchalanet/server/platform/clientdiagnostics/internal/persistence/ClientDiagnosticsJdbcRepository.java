package com.tchalanet.server.platform.clientdiagnostics.internal.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticCategory;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticEventDetailView;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticEventRequest;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticEventView;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticIngestionContext;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticPolicyView;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticSeverity;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ClientDiagnosticsJdbcRepository {

  private final JdbcTemplate jdbc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public ClientDiagnosticPolicyView getPolicy(
      TenantId tenantId, SellerTerminalId sellerTerminalId) {
    try {
      return jdbc.queryForObject(
          """
          SELECT enabled, expires_at, max_events, categories, reason, updated_at
            FROM client_diagnostic_policy
           WHERE tenant_id = ? AND seller_terminal_id = ? AND deleted_at IS NULL
          """,
          (rs, rowNum) -> policy(rs),
          tenantId.value(),
          sellerTerminalId.value());
    } catch (EmptyResultDataAccessException ignored) {
      return ClientDiagnosticPolicyView.disabled();
    }
  }

  public void enablePolicy(
      TenantId tenantId,
      SellerTerminalId sellerTerminalId,
      Instant expiresAt,
      int maxEvents,
      Set<ClientDiagnosticCategory> categories,
      String reason,
      UUID actorUserId) {
    var categoryNames = categories.stream().map(Enum::name).toArray(String[]::new);
    jdbc.execute(
        (org.springframework.jdbc.core.ConnectionCallback<Void>)
        connection -> {
          Array categoryArray = connection.createArrayOf("text", categoryNames);
          try (var ps =
              connection.prepareStatement(
                  """
                  INSERT INTO client_diagnostic_policy (
                    tenant_id, seller_terminal_id, enabled, expires_at, max_events, categories, reason, created_by, updated_by
                  )
                  VALUES (?, ?, true, ?, ?, ?, ?, ?, ?)
                  ON CONFLICT (tenant_id, seller_terminal_id)
                  DO UPDATE SET
                    enabled = true,
                    expires_at = EXCLUDED.expires_at,
                    max_events = EXCLUDED.max_events,
                    categories = EXCLUDED.categories,
                    reason = EXCLUDED.reason,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = now(),
                    version = client_diagnostic_policy.version + 1
                  """)) {
            ps.setObject(1, tenantId.value());
            ps.setObject(2, sellerTerminalId.value());
            ps.setTimestamp(3, Timestamp.from(expiresAt));
            ps.setInt(4, maxEvents);
            ps.setArray(5, categoryArray);
            ps.setString(6, reason);
            ps.setObject(7, actorUserId);
            ps.setObject(8, actorUserId);
            ps.executeUpdate();
          } finally {
            categoryArray.free();
          }
          return null;
        });
  }

  public void disablePolicy(
      TenantId tenantId, SellerTerminalId sellerTerminalId, UUID actorUserId) {
    jdbc.update(
        """
        INSERT INTO client_diagnostic_policy (
          tenant_id, seller_terminal_id, enabled, max_events, categories, updated_by
        )
        VALUES (?, ?, false, 100, ARRAY[]::text[], ?)
        ON CONFLICT (tenant_id, seller_terminal_id)
        DO UPDATE SET
          enabled = false,
          updated_by = EXCLUDED.updated_by,
          updated_at = now(),
          version = client_diagnostic_policy.version + 1
        """,
        tenantId.value(),
        sellerTerminalId.value(),
        actorUserId);
  }

  public int insertEvent(
      ClientDiagnosticIngestionContext context, ClientDiagnosticEventRequest event) {
    return jdbc.update(
        """
        INSERT INTO client_diagnostic_event (
          tenant_id, seller_terminal_id, event_id, category, severity, operation,
          occurred_at_client, received_at_server, request_id, correlation_id, error_code,
          message, exception_type, http_status, endpoint_key, app_version, build_number,
          platform, device_model, os_version, printer_provider, printer_service, printer_state,
          stack_frames
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
        ON CONFLICT (tenant_id, seller_terminal_id, event_id) DO NOTHING
        """,
        context.tenantId().value(),
        context.sellerTerminalId().value(),
        event.eventId(),
        event.category().name(),
        event.severity().name(),
        event.operation(),
        Timestamp.from(event.occurredAtClient()),
        Timestamp.from(context.receivedAtServer()),
        event.requestId(),
        event.correlationId(),
        event.errorCode(),
        event.message(),
        event.exceptionType(),
        event.httpStatus(),
        event.endpointKey(),
        event.appVersion(),
        event.buildNumber(),
        event.platform(),
        event.deviceModel(),
        event.osVersion(),
        event.printerProvider(),
        event.printerService(),
        event.printerState(),
        stackFramesJson(event));
  }

  public List<ClientDiagnosticEventView> recentEvents(
      TenantId tenantId, SellerTerminalId sellerTerminalId, int limit) {
    var safeLimit = Math.max(1, Math.min(limit, 200));
    var params = new ArrayList<>();
    var sql =
        new StringBuilder(
            """
        SELECT id, tenant_id, seller_terminal_id, event_id, category, severity, operation,
               occurred_at_client, received_at_server, request_id, correlation_id, error_code,
               message, exception_type, http_status, endpoint_key, app_version, platform,
               device_model, printer_provider, printer_state
          FROM client_diagnostic_event
         WHERE deleted_at IS NULL
        """);
    if (tenantId != null) {
      sql.append(" AND tenant_id = ?");
      params.add(tenantId.value());
    }
    if (sellerTerminalId != null) {
      sql.append(" AND seller_terminal_id = ?");
      params.add(sellerTerminalId.value());
    }
    sql.append(" ORDER BY received_at_server DESC LIMIT ?");
    params.add(safeLimit);
    return jdbc.query(sql.toString(), (rs, rowNum) -> event(rs), params.toArray());
  }

  public ClientDiagnosticEventDetailView eventDetail(UUID eventId) {
    return jdbc.queryForObject(
        """
        SELECT id, tenant_id, seller_terminal_id, event_id, category, severity, operation,
               occurred_at_client, received_at_server, request_id, correlation_id, error_code,
               message, exception_type, http_status, endpoint_key, app_version, build_number,
               platform, device_model, os_version, printer_provider, printer_service, printer_state,
               stack_frames
          FROM client_diagnostic_event
         WHERE id = ? AND deleted_at IS NULL
        """,
        (rs, rowNum) -> eventDetail(rs),
        eventId);
  }

  public int countEventsSince(TenantId tenantId, SellerTerminalId sellerTerminalId, Instant since) {
    if (since == null) return 0;
    var count =
        jdbc.queryForObject(
            """
        SELECT count(*)
          FROM client_diagnostic_event
         WHERE tenant_id = ? AND seller_terminal_id = ? AND received_at_server >= ? AND deleted_at IS NULL
        """,
            Integer.class,
            tenantId.value(),
            sellerTerminalId.value(),
            Timestamp.from(since));
    return count == null ? 0 : count;
  }

  public void purgeEventsOlderThan(Duration retention) {
    jdbc.update(
        """
        UPDATE client_diagnostic_event
           SET deleted_at = now()
         WHERE deleted_at IS NULL AND received_at_server < now() - (? * interval '1 second')
        """,
        retention.toSeconds());
  }

  private String stackFramesJson(ClientDiagnosticEventRequest event) {
    try {
      return objectMapper.writeValueAsString(
          event.stackFrames() == null ? List.of() : event.stackFrames());
    } catch (JsonProcessingException e) {
      return "[]";
    }
  }

  private static ClientDiagnosticPolicyView policy(ResultSet rs) throws SQLException {
    return new ClientDiagnosticPolicyView(
        rs.getBoolean("enabled"),
        instant(rs, "expires_at"),
        rs.getInt("max_events"),
        categories(rs.getArray("categories")),
        rs.getString("reason"),
        instant(rs, "updated_at"));
  }

  private static ClientDiagnosticEventView event(ResultSet rs) throws SQLException {
    return new ClientDiagnosticEventView(
        rs.getString("id"),
        TenantId.of(UUID.fromString(rs.getString("tenant_id"))),
        SellerTerminalId.of(UUID.fromString(rs.getString("seller_terminal_id"))),
        rs.getString("event_id"),
        ClientDiagnosticCategory.valueOf(rs.getString("category")),
        ClientDiagnosticSeverity.valueOf(rs.getString("severity")),
        rs.getString("operation"),
        instant(rs, "occurred_at_client"),
        instant(rs, "received_at_server"),
        rs.getString("request_id"),
        rs.getString("correlation_id"),
        rs.getString("error_code"),
        rs.getString("message"),
        rs.getString("exception_type"),
        (Integer) rs.getObject("http_status"),
        rs.getString("endpoint_key"),
        rs.getString("app_version"),
        rs.getString("platform"),
        rs.getString("device_model"),
        rs.getString("printer_provider"),
        rs.getString("printer_state"));
  }

  private ClientDiagnosticEventDetailView eventDetail(ResultSet rs) throws SQLException {
    return new ClientDiagnosticEventDetailView(
        rs.getString("id"),
        TenantId.of(UUID.fromString(rs.getString("tenant_id"))),
        SellerTerminalId.of(UUID.fromString(rs.getString("seller_terminal_id"))),
        rs.getString("event_id"),
        ClientDiagnosticCategory.valueOf(rs.getString("category")),
        ClientDiagnosticSeverity.valueOf(rs.getString("severity")),
        rs.getString("operation"),
        instant(rs, "occurred_at_client"),
        instant(rs, "received_at_server"),
        rs.getString("request_id"),
        rs.getString("correlation_id"),
        rs.getString("error_code"),
        rs.getString("message"),
        rs.getString("exception_type"),
        (Integer) rs.getObject("http_status"),
        rs.getString("endpoint_key"),
        rs.getString("app_version"),
        rs.getString("build_number"),
        rs.getString("platform"),
        rs.getString("device_model"),
        rs.getString("os_version"),
        rs.getString("printer_provider"),
        rs.getString("printer_service"),
        rs.getString("printer_state"),
        stackFrames(rs.getString("stack_frames")));
  }

  private List<com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticStackFrame>
      stackFrames(String json) {
    if (json == null || json.isBlank()) return List.of();
    try {
      var type =
          objectMapper
              .getTypeFactory()
              .constructCollectionType(
                  List.class,
                  com.tchalanet.server.platform.clientdiagnostics.api.model
                      .ClientDiagnosticStackFrame.class);
      return objectMapper.readValue(json, type);
    } catch (Exception ignored) {
      return List.of();
    }
  }

  private static Instant instant(ResultSet rs, String column) throws SQLException {
    var timestamp = rs.getTimestamp(column);
    return timestamp == null ? null : timestamp.toInstant();
  }

  private static Set<ClientDiagnosticCategory> categories(Array array) throws SQLException {
    if (array == null) return Set.of();
    return Arrays.stream((String[]) array.getArray())
        .map(ClientDiagnosticCategory::valueOf)
        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
  }
}
