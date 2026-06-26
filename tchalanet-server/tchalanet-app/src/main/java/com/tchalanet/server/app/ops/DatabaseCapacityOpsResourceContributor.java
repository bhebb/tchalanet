package com.tchalanet.server.app.ops;

import com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin.OpsResourceContributor;
import com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin.PlatformAdminOpsDashboardPayloadAssembler;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(130)
public class DatabaseCapacityOpsResourceContributor implements OpsResourceContributor {

  private static final List<String> DEFAULT_SCHEMAS = List.of("public", "batch");

  private final JdbcTemplate jdbc;
  private final int warningMb;
  private final int criticalMb;

  public DatabaseCapacityOpsResourceContributor(
      DataSource dataSource,
      @Value("${tch.ops.database.schema-warning-mb:1024}") int warningMb,
      @Value("${tch.ops.database.schema-critical-mb:2048}") int criticalMb) {
    this.jdbc = new JdbcTemplate(dataSource);
    this.warningMb = Math.max(1, warningMb);
    this.criticalMb = Math.max(this.warningMb + 1, criticalMb);
  }

  @Override
  public List<PlatformAdminOpsDashboardPayloadAssembler.OpsServiceResourceItem> services() {
    String sql = """
        SELECT
          n.nspname AS schema_name,
          COUNT(c.oid) FILTER (WHERE c.relkind IN ('r', 'p')) AS table_count,
          COALESCE(SUM(pg_total_relation_size(c.oid)) FILTER (WHERE c.relkind IN ('r', 'p')), 0) AS total_bytes,
          COALESCE(SUM(pg_indexes_size(c.oid)) FILTER (WHERE c.relkind IN ('r', 'p')), 0) AS index_bytes
        FROM pg_namespace n
        LEFT JOIN pg_class c ON c.relnamespace = n.oid
        WHERE n.nspname IN ('public', 'batch')
        GROUP BY n.nspname
        ORDER BY n.nspname
        """;

    return jdbc.query(sql, (rs, rowNum) -> {
      String schema = rs.getString("schema_name");
      int tableCount = rs.getInt("table_count");
      int sizeMb = toMb(rs.getLong("total_bytes"));
      int indexMb = toMb(rs.getLong("index_bytes"));
      String severity = severity(sizeMb);
      String status = switch (severity) {
        case "CRITICAL" -> "CRITICAL";
        case "WARNING" -> "HIGH";
        default -> "OK";
      };

      return new PlatformAdminOpsDashboardPayloadAssembler.OpsServiceResourceItem(
          "database:schema:" + schema,
          "DB schema " + schema,
          status,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          severity,
          "Schema " + schema + " uses " + sizeMb + " MB across " + tableCount + " tables.",
          "/app/platform/ops/resources",
          sizeMb,
          indexMb,
          tableCount);
    }).stream()
        .sorted((a, b) -> schemaOrder(a.serviceKey()) - schemaOrder(b.serviceKey()))
        .toList();
  }

  private String severity(int sizeMb) {
    if (sizeMb >= criticalMb) return "CRITICAL";
    if (sizeMb >= warningMb) return "WARNING";
    return "OK";
  }

  private static int toMb(long bytes) {
    if (bytes <= 0) return 0;
    return (int) Math.max(1, Math.ceil(bytes / 1024.0 / 1024.0));
  }

  private static int schemaOrder(String serviceKey) {
    String schema = serviceKey == null ? "" : serviceKey.replace("database:schema:", "");
    int index = DEFAULT_SCHEMAS.indexOf(schema);
    return index >= 0 ? index : DEFAULT_SCHEMAS.size();
  }
}
