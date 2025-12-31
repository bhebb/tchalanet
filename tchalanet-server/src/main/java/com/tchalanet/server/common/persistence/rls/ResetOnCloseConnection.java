package com.tchalanet.server.common.infra.persistence.rls;

import java.lang.reflect.*;
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
                Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                handler);
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
                try (PreparedStatement stTenant =
                         delegate.prepareStatement("select set_config('app.current_tenant', '', true)");
                     PreparedStatement stVis =
                         delegate.prepareStatement("select set_config('app.deleted_visibility', 'active', true)")) {
                    stTenant.execute();
                    stVis.execute();
                } catch (SQLException ex) {
                    log.warn("Failed to reset RLS context for connection", ex);
                }
            }

            delegate.close();
        }
    }
}
