package com.tchalanet.server.common.persistence.rls;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RlsAwareDataSourceTest {

  @AfterEach
  void clearContext() {
    TchContext.clear();
  }

  @Test
  void appliesServerContextAndResetsEveryRlsVariableOnClose() throws Exception {
    var jdbc = new RecordingJdbc();
    var tenantId = UUID.randomUUID();
    TchContext.set(context(tenantId, ApiScope.ADMIN, true));

    var connection =
        new RlsAwareDataSource(jdbc.dataSource(), new TchContextResolver()).getConnection();
    connection.close();

    assertThat(jdbc.setConfigs())
        .containsSubsequence(
            new SetConfig("app.current_tenant", tenantId.toString()),
            new SetConfig("app.deleted_visibility", "active"),
            new SetConfig("app.api_scope", "admin"),
            new SetConfig("app.is_super_admin", "true"))
        .endsWith(
            new SetConfig("app.current_tenant", ""),
            new SetConfig("app.deleted_visibility", "active"),
            new SetConfig("app.api_scope", ""),
            new SetConfig("app.is_super_admin", "false"));
    assertThat(jdbc.delegateClosed).isTrue();
  }

  @Test
  void missingContextFailsClosedWithEmptyTenantAndNoSuperAdmin() throws Exception {
    var jdbc = new RecordingJdbc();

    try (var ignored =
        new RlsAwareDataSource(jdbc.dataSource(), new TchContextResolver()).getConnection()) {
      assertThat(jdbc.setConfigs())
          .containsSubsequence(
              new SetConfig("app.current_tenant", ""),
              new SetConfig("app.deleted_visibility", "active"),
              new SetConfig("app.api_scope", ""),
              new SetConfig("app.is_super_admin", "false"));
    }
  }

  private static TchRequestContext context(UUID tenantUuid, ApiScope scope, boolean superAdmin) {
    return new TchRequestContext(
        "tenant",
        tenantUuid,
        "tenant",
        tenantUuid,
        "external-subject",
        UUID.randomUUID(),
        superAdmin ? Set.of(TchRole.SUPER_ADMIN) : Set.of(),
        Set.of(),
        Locale.CANADA_FRENCH,
        "request-id",
        "127.0.0.1",
        "test",
        false,
        null,
        "active",
        scope,
        null,
        TenantId.of(tenantUuid),
        ZoneId.of("America/Port-au-Prince"),
        Currency.getInstance("HTG"),
        null,
        null, null, null, null, null);
  }

  private record SetConfig(String key, String value) {}

  private static final class RecordingJdbc {
    private final List<SetConfig> setConfigs = new ArrayList<>();
    private boolean delegateClosed;

    DataSource dataSource() {
      return proxy(
          DataSource.class,
          (ignored, method, args) -> {
            if ("getConnection".equals(method.getName())) return connection();
            return defaultValue(method.getReturnType());
          });
    }

    List<SetConfig> setConfigs() {
      return setConfigs;
    }

    private Connection connection() {
      return proxy(
          Connection.class,
          (ignored, method, args) -> {
            if ("prepareStatement".equals(method.getName())) {
              return statement((String) args[0]);
            }
            if ("close".equals(method.getName())) {
              delegateClosed = true;
              return null;
            }
            return defaultValue(method.getReturnType());
          });
    }

    private PreparedStatement statement(String sql) {
      var parameter = new String[1];
      return proxy(
          PreparedStatement.class,
          (ignored, method, args) -> {
            if ("setString".equals(method.getName())) {
              parameter[0] = (String) args[1];
              return null;
            }
            if ("execute".equals(method.getName()) || "executeQuery".equals(method.getName())) {
              recordSetConfig(sql, parameter[0]);
              return "executeQuery".equals(method.getName()) ? resultSet() : true;
            }
            return defaultValue(method.getReturnType());
          });
    }

    private void recordSetConfig(String sql, String parameter) {
      if (!sql.contains("set_config('app.")) return;
      int start = sql.indexOf("set_config('") + "set_config('".length();
      int end = sql.indexOf("'", start);
      String value = parameter;
      if (value == null) {
        int valueStart = sql.indexOf("'", end + 1) + 1;
        int valueEnd = sql.indexOf("'", valueStart);
        value = sql.substring(valueStart, valueEnd);
      }
      setConfigs.add(new SetConfig(sql.substring(start, end), value));
    }

    private ResultSet resultSet() {
      return proxy(
          ResultSet.class,
          (ignored, method, args) -> {
            if ("next".equals(method.getName())) return true;
            if ("getString".equals(method.getName())) return "";
            return defaultValue(method.getReturnType());
          });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
      return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
    }

    private static Object defaultValue(Class<?> type) {
      if (!type.isPrimitive()) return null;
      if (type == boolean.class) return false;
      if (type == byte.class) return (byte) 0;
      if (type == short.class) return (short) 0;
      if (type == int.class) return 0;
      if (type == long.class) return 0L;
      if (type == float.class) return 0F;
      if (type == double.class) return 0D;
      if (type == char.class) return '\0';
      return null;
    }
  }
}
