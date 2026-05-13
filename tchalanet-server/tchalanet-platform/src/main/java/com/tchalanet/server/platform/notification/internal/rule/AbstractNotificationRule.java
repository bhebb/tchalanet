package com.tchalanet.server.platform.notification.internal.rule;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

abstract class AbstractNotificationRule implements NotificationRule {

  protected boolean simpleName(Object event, String simpleName) {
    return event != null && event.getClass().getSimpleName().equals(simpleName);
  }

  protected boolean simpleNameStartsWith(Object event, String prefix) {
    return event != null && event.getClass().getSimpleName().startsWith(prefix);
  }

  protected NotificationIntent intent(
      Object event,
      String templateKey,
      NotificationSeverity severity,
      NotificationKind kind,
      NotificationCategory category,
      String title,
      String message) {
    var eventId = uuidValue(event, "eventId");
    var tenantId = tenantId(event);
    var correlationKey = handlerKey() + ":" + (eventId == null ? event.hashCode() : eventId);
    return new NotificationIntent(
        eventId,
        tenantId,
        event.getClass().getSimpleName(),
        templateKey,
        severity,
        kind,
        category,
        NotificationAudienceType.ROLE,
        "TENANT_ADMIN",
        variables(event),
        title,
        message,
        correlationKey);
  }

  protected NotificationIntent platformIntent(
      Object event,
      String templateKey,
      NotificationSeverity severity,
      NotificationKind kind,
      NotificationCategory category,
      String title,
      String message) {
    var eventId = uuidValue(event, "eventId");
    var correlationKey = handlerKey() + ":" + (eventId == null ? event.hashCode() : eventId);
    return new NotificationIntent(
        eventId,
        null,
        event.getClass().getSimpleName(),
        templateKey,
        severity,
        kind,
        category,
        NotificationAudienceType.PLATFORM,
        "platform",
        variables(event),
        title,
        message,
        correlationKey);
  }

  private TenantId tenantId(Object event) {
    var tenantId = value(event, "tenantId");
    if (tenantId instanceof TenantId typed) {
      return typed;
    }
    if (tenantId instanceof UUID uuid) {
      return TenantId.of(uuid);
    }
    return null;
  }

  private UUID uuidValue(Object event, String methodName) {
    var value = value(event, methodName);
    if (value instanceof UUID uuid) {
      return uuid;
    }
    if (value != null) {
      try {
        return UUID.fromString(value.toString());
      } catch (IllegalArgumentException ignored) {
        return null;
      }
    }
    return null;
  }

  private Map<String, Object> variables(Object event) {
    var variables = new LinkedHashMap<String, Object>();
    for (Method method : event.getClass().getMethods()) {
      if (method.getParameterCount() == 0
          && method.getDeclaringClass() != Object.class
          && !method.getName().equals("toString")
          && !method.getName().equals("hashCode")) {
        try {
          variables.put(method.getName(), method.invoke(event));
        } catch (ReflectiveOperationException ignored) {
          // Best-effort event facts for templates.
        }
      }
    }
    return variables;
  }

  private Object value(Object event, String methodName) {
    try {
      return event.getClass().getMethod(methodName).invoke(event);
    } catch (ReflectiveOperationException ignored) {
      return null;
    }
  }
}
