package com.tchalanet.server.app.ops;

import com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin.OpsResourceContributor;
import com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin.PlatformAdminOpsDashboardPayloadAssembler;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class JvmMemoryOpsResourceContributor implements OpsResourceContributor {

  @Override
  public List<PlatformAdminOpsDashboardPayloadAssembler.OpsServiceResourceItem> services() {
    Runtime runtime = Runtime.getRuntime();
    long usedBytes = runtime.totalMemory() - runtime.freeMemory();
    long maxBytes = runtime.maxMemory();
    Integer usedMb = toMb(usedBytes);
    Integer limitMb = maxBytes <= 0 || maxBytes == Long.MAX_VALUE ? null : toMb(maxBytes);
    Integer percent = limitMb == null || limitMb == 0
        ? null
        : (int) Math.round((usedBytes * 100.0) / maxBytes);

    String severity = severity(percent);
    String status = switch (severity) {
      case "CRITICAL" -> "CRITICAL";
      case "WARNING" -> "HIGH";
      default -> "OK";
    };
    String message = switch (severity) {
      case "CRITICAL" -> "JVM memory is critically high.";
      case "WARNING" -> "JVM memory is elevated.";
      default -> "JVM memory is within the expected range.";
    };

    return List.of(new PlatformAdminOpsDashboardPayloadAssembler.OpsServiceResourceItem(
        "runtime:jvm-memory",
        "JVM memory",
        status,
        usedMb,
        limitMb,
        percent,
        null,
        null,
        null,
        null,
        severity,
        message,
        "/app/platform/ops/resources",
        null,
        null,
        null));
  }

  private static Integer toMb(long bytes) {
    return (int) Math.max(0, Math.round(bytes / 1024.0 / 1024.0));
  }

  private static String severity(Integer percent) {
    if (percent == null) return "WARNING";
    if (percent >= 90) return "CRITICAL";
    if (percent >= 75) return "WARNING";
    return "OK";
  }
}
