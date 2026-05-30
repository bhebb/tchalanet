package com.tchalanet.server.platform.tenantconfig.internal.persistence;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenantconfig.api.model.TenantBusinessDayView;
import com.tchalanet.server.platform.tenantconfig.internal.port.TenantBusinessCalendarOverrideReader;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class JdbcTenantBusinessCalendarOverrideReader implements TenantBusinessCalendarOverrideReader {

    private static final String OUTLET_SQL = """
        SELECT open, reason_code, label
        FROM business_day_override
        WHERE tenant_id = :tenant_id
          AND outlet_id = :outlet_id
          AND business_date = :business_date
          AND deleted_at IS NULL
        LIMIT 1
        """;

    private static final String TENANT_SQL = """
        SELECT open, reason_code, label
        FROM business_day_override
        WHERE tenant_id = :tenant_id
          AND outlet_id IS NULL
          AND business_date = :business_date
          AND deleted_at IS NULL
        LIMIT 1
        """;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Optional<TenantBusinessDayView> findOutletOverride(
        TenantId tenantId,
        OutletId outletId,
        LocalDate date
    ) {
        var params = new MapSqlParameterSource()
            .addValue("tenant_id", tenantId.value())
            .addValue("outlet_id", outletId.value())
            .addValue("business_date", date);

        return jdbc.query(OUTLET_SQL, params, (rs, i) ->
            new TenantBusinessDayView(
                tenantId,
                date,
                rs.getBoolean("open"),
                rs.getString("reason_code"),
                rs.getString("label")
            )
        ).stream().findFirst();
    }

    @Override
    public Optional<TenantBusinessDayView> findTenantOverride(
        TenantId tenantId,
        LocalDate date
    ) {
        var params = new MapSqlParameterSource()
            .addValue("tenant_id", tenantId.value())
            .addValue("business_date", date);

        return jdbc.query(TENANT_SQL, params, (rs, i) ->
            new TenantBusinessDayView(
                tenantId,
                date,
                rs.getBoolean("open"),
                rs.getString("reason_code"),
                rs.getString("label")
            )
        ).stream().findFirst();
    }
}
