package com.tchalanet.server.core.subscription.infra.persistence.read;

import com.tchalanet.server.core.subscription.application.query.model.PlatformSubscriptionStatsView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DisplayName("JdbcSubscriptionStatsReader — integration (Testcontainers PostgreSQL)")
class JdbcSubscriptionStatsReaderIT {

    @Container
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17")
            .withUsername("test")
            .withPassword("test")
            .withDatabaseName("testdb");

    JdbcTemplate jdbc;
    JdbcSubscriptionStatsReader reader;

    @BeforeEach
    void setup() {
        var ds = new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        jdbc = new JdbcTemplate(ds);
        reader = new JdbcSubscriptionStatsReader(jdbc);

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS tenant_subscription (
                id         uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
                tenant_id  uuid        NOT NULL,
                plan_code  varchar(50) NOT NULL,
                status     varchar(20) NOT NULL,
                created_at timestamptz DEFAULT now(),
                deleted_at timestamptz
            )
            """);
    }

    @AfterEach
    void cleanup() {
        jdbc.execute("DROP TABLE IF EXISTS tenant_subscription");
    }

    @Test
    @DisplayName("returns correct aggregates when table contains rows")
    void readPlatformStats_withData_returnsCorrectAggregates() {
        jdbc.execute("""
            INSERT INTO tenant_subscription (tenant_id, plan_code, status) VALUES
                (gen_random_uuid(), 'BASIC', 'ACTIVE'),
                (gen_random_uuid(), 'BASIC', 'ACTIVE'),
                (gen_random_uuid(), 'PRO',   'CANCELED'),
                (gen_random_uuid(), 'PRO',   'PAST_DUE')
            """);

        PlatformSubscriptionStatsView stats = reader.readPlatformStats();

        assertThat(stats.total()).isEqualTo(4);
        assertThat(stats.active()).isEqualTo(2);
        assertThat(stats.canceled()).isEqualTo(1);
        assertThat(stats.pastDue()).isEqualTo(1);
        assertThat(stats.byPlan()).hasSize(2);

        var basicRow = stats.byPlan().stream()
            .filter(r -> "BASIC".equals(r.planCode())).findFirst().orElseThrow();
        assertThat(basicRow.total()).isEqualTo(2);
        assertThat(basicRow.active()).isEqualTo(2);

        var proRow = stats.byPlan().stream()
            .filter(r -> "PRO".equals(r.planCode())).findFirst().orElseThrow();
        assertThat(proRow.total()).isEqualTo(2);
        assertThat(proRow.active()).isEqualTo(0);
    }

    @Test
    @DisplayName("returns all-zero stats when table is empty")
    void readPlatformStats_withEmptyTable_returnsZeros() {
        PlatformSubscriptionStatsView stats = reader.readPlatformStats();

        assertThat(stats.total()).isZero();
        assertThat(stats.active()).isZero();
        assertThat(stats.pastDue()).isZero();
        assertThat(stats.canceled()).isZero();
        assertThat(stats.byPlan()).isEmpty();
    }

    @Test
    @DisplayName("does not throw — table name is tenant_subscription, not subscription")
    void readPlatformStats_tableNameIsCorrect_noException() {
        assertThat(reader.readPlatformStats()).isNotNull();
    }
}
