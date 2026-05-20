package com.tchalanet.server.common.web.advice;

import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.common.web.api.ServiceHealth;
import com.tchalanet.server.common.web.api.ServiceStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Request-scoped context for collecting API response notices and service statuses. Uses ThreadLocal
 * to maintain state per HTTP request.
 */
public class ApiResponseContext {

  private static final ThreadLocal<ApiResponseContext> CONTEXT =
      ThreadLocal.withInitial(ApiResponseContext::new);

  private final List<ApiNotice> notices = new ArrayList<>();
  private final List<ServiceStatus> services = new ArrayList<>();

  private ApiResponseContext() {}

  public static ApiResponseContext get() {
    return CONTEXT.get();
  }

  public static void clear() {
    CONTEXT.remove();
  }

  public void addNotice(ApiNotice notice) {
    notices.add(notice);
  }

  public void addNotice(String code, String message, String domain, NoticeSeverity severity) {
    addNotice(new ApiNotice(code, message, domain, severity, java.util.Map.of()));
  }

  public void addServiceStatus(ServiceStatus serviceStatus) {
    services.add(serviceStatus);
  }

  public void addServiceStatus(String service, ServiceHealth status, String message) {
    addServiceStatus(new ServiceStatus(service, status, message));
  }

  public List<ApiNotice> getNotices() {
    return new ArrayList<>(notices);
  }

  public List<ServiceStatus> getServices() {
    return new ArrayList<>(services);
  }

  public boolean hasWarnings() {
    return notices.stream().anyMatch(n -> n.severity() == NoticeSeverity.WARN);
  }

  public boolean hasDegradedServices() {
    return services.stream().anyMatch(s -> s.status() != ServiceHealth.UP);
  }
}
