package com.tchalanet.server.common.infra.persistence.rls;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.TchRequestContextHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.ScopeNotActiveException;
import org.springframework.jdbc.datasource.DelegatingDataSource;

@Slf4j
public final class RlsAwareDataSource extends DelegatingDataSource {

    private final ObjectProvider<TchRequestContextHolder> holderProvider;
    private final TenantUuidLookupJdbc tenantLookup;

    public RlsAwareDataSource(
        DataSource targetDataSource,
        ObjectProvider<TchRequestContextHolder> holderProvider,
        TenantUuidLookupJdbc tenantLookup) {
        super(targetDataSource);
        this.holderProvider = holderProvider;
        this.tenantLookup = tenantLookup;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection raw = super.getConnection();
        applyOrResetRls(raw);
        return ResetOnCloseConnection.wrap(raw, true);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection raw = super.getConnection(username, password);
        applyOrResetRls(raw);
        return ResetOnCloseConnection.wrap(raw, true);
    }

    private void applyOrResetRls(Connection conn) {
        UUID tenantUuid = null;
        String visibility = "active";

        try {
            var holder = holderProvider.getIfAvailable();
            if (holder != null) {
                TchRequestContext ctx = holder.get(); // peut throw si pas de request active
                if (ctx != null) {
                    visibility = ctx.deletedVisibilitySafe();

                    // 1) UUID si déjà connu
                    tenantUuid = ctx.tenantUuid();

                    // 2) sinon, lookup JDBC raw à partir du code
                    if (tenantUuid == null) {
                        var code = ctx.effectiveTenantCode();
                        tenantUuid = tenantLookup.findTenantUuidByCode(code).orElse(null);
                    }
                }
            }
        } catch (ScopeNotActiveException | IllegalStateException e) {
            tenantUuid = null; // normal hors web request (startup/batch)
        } catch (Exception e) {
            log.debug("RLS context read failed", e);
            tenantUuid = null;
        }

        if (tenantUuid == null) {
            resetViaSetConfig(conn);
            return;
        }

        applyViaSetConfig(conn, tenantUuid, visibility);
    }

    private void applyViaSetConfig(Connection conn, UUID tenantUuid, String visibility) {
        try (PreparedStatement stTenant =
                 conn.prepareStatement("select set_config('app.current_tenant', ?, true)");
             PreparedStatement stVis =
                 conn.prepareStatement("select set_config('app.deleted_visibility', ?, true)")) {

            stTenant.setString(1, tenantUuid.toString());
            stTenant.execute();

            stVis.setString(1, visibility);
            stVis.execute();

        } catch (Exception e) {
            log.error("Failed to apply RLS via set_config (tenant={}, visibility={})", tenantUuid, visibility, e);
            resetViaSetConfig(conn);
        }
    }

    private void resetViaSetConfig(Connection conn) {
        try (PreparedStatement stTenant =
                 conn.prepareStatement("select set_config('app.current_tenant', '', true)");
             PreparedStatement stVis =
                 conn.prepareStatement("select set_config('app.deleted_visibility', 'active', true)")) {
            stTenant.execute();
            stVis.execute();
        } catch (Exception e) {
            // ne casse pas le boot
            log.debug("Failed to reset RLS via set_config", e);
        }
    }
}
