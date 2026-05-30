package com.tchalanet.server.core.session.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.session.internal.application.port.out.OutletOperationalSettingsReaderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class JdbcOutletOperationalSettingsAdapter implements OutletOperationalSettingsReaderPort {

    private static final String SQL = """
        SELECT timezone
        FROM outlet
        WHERE id         = :outlet_id
          AND tenant_id  = :tenant_id
          AND deleted_at IS NULL
        LIMIT 1
        """;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Optional<ZoneId> findOutletZone(TenantId tenantId, OutletId outletId) {
        var params = new MapSqlParameterSource()
            .addValue("outlet_id",  outletId.value())
            .addValue("tenant_id", tenantId.value());

        return jdbc.query(SQL, params, (rs, i) -> {
            var raw = rs.getString("timezone");
            if (raw == null || raw.isBlank()) return null;
            try {
                return ZoneId.of(raw);
            } catch (Exception e) {
                return null;
            }
        }).stream().filter(z -> z != null).findFirst();
    }
}
