package com.tchalanet.server.core.subscription.infra.persistence.read;

import com.tchalanet.server.core.subscription.application.port.out.SubscriptionStatsReaderPort;
import com.tchalanet.server.core.subscription.application.query.model.PlatformSubscriptionStatsView;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcSubscriptionStatsReader implements SubscriptionStatsReaderPort {

  private static final String TABLE = "tenant_subscription";

  private final JdbcTemplate jdbc;

  @Override
  public PlatformSubscriptionStatsView readPlatformStats() {

    int total    = queryCount("select count(*) from " + TABLE);
    int active   = queryCount("select count(*) from " + TABLE + " where status = 'ACTIVE'");
    int pastDue  = queryCount("select count(*) from " + TABLE + " where status = 'PAST_DUE'");
    int canceled = queryCount("select count(*) from " + TABLE + " where status = 'CANCELED'");

    List<PlatformSubscriptionStatsView.ByPlanRow> byPlan = jdbc.query(
        "select plan_code, count(*) as total, sum(case when status='ACTIVE' then 1 else 0 end) as active"
        + " from " + TABLE
        + " group by plan_code order by plan_code",
        (rs, i) -> new PlatformSubscriptionStatsView.ByPlanRow(
            rs.getString("plan_code"),
            rs.getInt("total"),
            rs.getInt("active")
        )
    );

    return new PlatformSubscriptionStatsView(total, active, pastDue, canceled, byPlan);
  }

  private int queryCount(String sql) {
    Integer v = jdbc.queryForObject(sql, Integer.class);
    return v == null ? 0 : v;
  }
}
