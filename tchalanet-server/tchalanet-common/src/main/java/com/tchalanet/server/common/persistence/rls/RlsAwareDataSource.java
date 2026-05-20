package com.tchalanet.server.common.persistence.rls;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.context.scope.ApiScope;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DelegatingDataSource;

@Slf4j
public final class RlsAwareDataSource extends DelegatingDataSource {

    private static final String DEFAULT_VISIBILITY = "active";
    private static final String DEFAULT_SCOPE = ""; // empty = treated as non-platform by helpers
    private static final String DEFAULT_IS_SUPER_ADMIN = "false";

    private final TchContextResolver resolver;

    public RlsAwareDataSource(DataSource targetDataSource, TchContextResolver resolver) {
        super(targetDataSource);
        this.resolver = resolver;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection raw = super.getConnection();
        applyOrResetRls(raw);
        logRlsVars(raw, "after-apply");
        return ResetOnCloseConnection.wrap(raw, true);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        var raw = super.getConnection(username, password);
        applyOrResetRls(raw);
        logRlsVars(raw, "after-apply");
        return ResetOnCloseConnection.wrap(raw, true);
    }

    private void logRlsVars(Connection conn, String when) {
        try (var ps =
                 conn.prepareStatement(
                     """
                         select
                           current_setting('app.current_tenant', true) as t,
                           current_setting('app.deleted_visibility', true) as v,
                           current_setting('app.api_scope', true) as s,
                           current_setting('app.is_super_admin', true) as sa
                     """)) {
            try (var rs = ps.executeQuery()) {
                rs.next();
                log.debug(
                    "[RLS {}] tenant='{}' visibility='{}' scope='{}' super_admin='{}'",
                    when,
                    rs.getString("t"),
                    rs.getString("v"),
                    rs.getString("s"),
                    rs.getString("sa"));
            }
        } catch (Exception e) {
            log.debug("[RLS {}] cannot read current_setting", when, e);
        }
    }

    private void applyOrResetRls(Connection conn) {
        UUID tenantUuid = null;
        String visibility = DEFAULT_VISIBILITY;
        String scope = DEFAULT_SCOPE;
        String isSuperAdmin = DEFAULT_IS_SUPER_ADMIN;

        try {
            var ctx = resolver.currentOrNull();
            if (ctx != null) {
                tenantUuid = ctx.tenantUuid();
                visibility = safeVisibility(ctx.deletedVisibilitySafe());

                // IMPORTANT: apiScope is now an enum in context
                ApiScope apiScope = ctx.apiScope();
                scope = (apiScope == null) ? DEFAULT_SCOPE : apiScope.name().toLowerCase();

                // IMPORTANT: super admin from roles (server-trusted)
                isSuperAdmin = ctx.isSuperAdmin() ? "true" : "false";
            }
        } catch (Exception e) {
            log.debug("RLS context read failed", e);
            tenantUuid = null;
            visibility = DEFAULT_VISIBILITY;
            scope = DEFAULT_SCOPE;
            isSuperAdmin = DEFAULT_IS_SUPER_ADMIN;
        }

        // If no tenant, we still want scope + super_admin set (for debugging),
        // but RLS functions current_tenant() should resolve to NULL.
        if (tenantUuid == null) {
            resetViaSetConfig(conn, scope, isSuperAdmin);
            logRlsVars(conn, "after-reset");
            return;
        }

        applyViaSetConfig(conn, tenantUuid, visibility, scope, isSuperAdmin);
        logRlsVars(conn, "after-apply-internal");
    }

    private void applyViaSetConfig(
        Connection conn,
        UUID tenantUuid,
        String visibility,
        String scope,
        String isSuperAdmin
    ) {
        try (PreparedStatement stTenant =
                 conn.prepareStatement("select set_config('app.current_tenant', ?, false)");
             PreparedStatement stVis =
                 conn.prepareStatement("select set_config('app.deleted_visibility', ?, false)");
             PreparedStatement stScope =
                 conn.prepareStatement("select set_config('app.api_scope', ?, false)");
             PreparedStatement stSa =
                 conn.prepareStatement("select set_config('app.is_super_admin', ?, false)")) {

            stTenant.setString(1, tenantUuid.toString());
            stTenant.execute();

            stVis.setString(1, visibility);
            stVis.execute();

            stScope.setString(1, scope);
            stScope.execute();

            stSa.setString(1, isSuperAdmin);
            stSa.execute();

        } catch (Exception e) {
            log.error(
                "Failed to apply RLS via set_config (tenant={}, visibility={}, scope={}, super_admin={})",
                tenantUuid,
                visibility,
                scope,
                isSuperAdmin,
                e);
            resetViaSetConfig(conn, scope, isSuperAdmin);
            logRlsVars(conn, "after-reset-error");
        }
    }

    private void resetViaSetConfig(Connection conn, String scope, String isSuperAdmin) {
        try (PreparedStatement stTenant =
                 conn.prepareStatement("select set_config('app.current_tenant', '', false)");
             PreparedStatement stVis =
                 conn.prepareStatement("select set_config('app.deleted_visibility', 'active', false)");
             PreparedStatement stScope =
                 conn.prepareStatement("select set_config('app.api_scope', ?, false)");
             PreparedStatement stSa =
                 conn.prepareStatement("select set_config('app.is_super_admin', ?, false)")) {

            stTenant.execute();
            stVis.execute();

            // Keep scope/sa visible for diagnostics even when tenant is null
            stScope.setString(1, scope == null ? DEFAULT_SCOPE : scope);
            stScope.execute();

            stSa.setString(1, isSuperAdmin == null ? DEFAULT_IS_SUPER_ADMIN : isSuperAdmin);
            stSa.execute();

        } catch (Exception e) {
            log.debug("Failed to reset RLS via set_config", e);
        }
    }

    private String safeVisibility(String v) {
        if (v == null) return DEFAULT_VISIBILITY;
        String x = v.trim().toLowerCase();
        return (x.equals("active") || x.equals("deleted") || x.equals("all")) ? x : DEFAULT_VISIBILITY;
    }

    private String safeScope(String s) {
        if (s == null) return DEFAULT_SCOPE;
        var x = s.trim().toLowerCase();
        // we only care about "platform" vs not-platform in SQL helpers; keep other values for logs
        return x;
    }
}
