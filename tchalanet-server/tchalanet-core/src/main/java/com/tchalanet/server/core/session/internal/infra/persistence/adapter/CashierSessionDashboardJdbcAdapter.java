package com.tchalanet.server.core.session.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.api.query.CashierIdentityView;
import com.tchalanet.server.core.session.internal.application.port.out.CashierSessionDashboardReaderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CashierSessionDashboardJdbcAdapter implements CashierSessionDashboardReaderPort {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String IDENTITY_SQL = """
        SELECT display_name AS cashier_display_name
        FROM app_user
        WHERE id = :cashierId
        LIMIT 1
        """;

    @Override
    public CashierIdentityView findIdentity(TenantId tenantId, UserId cashierId) {
        var params = new MapSqlParameterSource().addValue("cashierId", cashierId.value());
        List<String> names = jdbc.query(IDENTITY_SQL, params,
            (rs, i) -> rs.getString("cashier_display_name"));
        String displayName = names.isEmpty() ? null : names.get(0);
        return new CashierIdentityView(displayName, null, null, tenantId.value().toString());
    }
}
