package com.tchalanet.server.common.persistence.rls;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ResetOnCloseConnection {

    private ResetOnCloseConnection() {}

    public static Connection wrap(Connection delegate, boolean enabled) {
        InvocationHandler handler = new ResetOnCloseHandler(delegate, enabled);
        return (Connection)
            Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[] {Connection.class}, handler);
    }

    private static final class ResetOnCloseHandler implements InvocationHandler {
        private final Connection delegate;
        private final boolean enabled;
        private boolean closed;

        private ResetOnCloseHandler(Connection delegate, boolean enabled) {
            this.delegate = delegate;
            this.enabled = enabled;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("close".equals(method.getName())) {
                close();
                return null;
            }
            return method.invoke(delegate, args);
        }

        private void close() throws SQLException {
            if (closed) return;
            closed = true;

            if (enabled) {
                // IMPORTANT: must reset ALL app.* session variables used by RLS helpers/policies,
                // otherwise a pooled connection can leak scope/superadmin/visibility across requests.
                try (PreparedStatement stTenant =
                         delegate.prepareStatement("select set_config('app.current_tenant', '', false)");
                     PreparedStatement stVis =
                         delegate.prepareStatement("select set_config('app.deleted_visibility', 'active', false)");
                     PreparedStatement stScope =
                         delegate.prepareStatement("select set_config('app.api_scope', '', false)");
                     PreparedStatement stSa =
                         delegate.prepareStatement("select set_config('app.is_super_admin', 'false', false)")) {

                    stTenant.execute();
                    stVis.execute();
                    stScope.execute();
                    stSa.execute();

                } catch (SQLException ex) {
                    log.debug("Failed to reset RLS context via set_config on close()", ex);
                }
            }

            delegate.close();
        }
    }
}
