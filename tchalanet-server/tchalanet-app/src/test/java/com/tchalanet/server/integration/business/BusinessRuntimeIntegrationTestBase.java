package com.tchalanet.server.integration.business;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.constant.CommonConstants;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.TchContextScope;
import com.tchalanet.server.common.context.operational.OperationalContextHint;
import com.tchalanet.server.common.context.operational.OperationalContextSource;
import com.tchalanet.server.common.context.operational.OperationalContextTrust;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.app.TchalanetApplication;
import com.tchalanet.server.platform.identity.internal.handoff.ProviderSessionTokenIssuerIntegrationTestConfig;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
    classes = {
        TchalanetApplication.class,
        ProviderSessionTokenIssuerIntegrationTestConfig.class,
        BusinessRuntimeIntegrationTestBase.FixedClockConfiguration.class
    },
    properties = {
        "tch.identity.provider=local-jwt",
        "tch.identity.local.secret=dev-only-change-me-at-least-32-characters",
        "tch.communication.edge.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "management.tracing.enabled=false",
        "management.otlp.metrics.export.enabled=false"
    })
@ContextConfiguration(initializers = BusinessRuntimeIntegrationTestBase.FlywayContainerInitializer.class)
abstract class BusinessRuntimeIntegrationTestBase {

    static final Instant FIXED_NOW = Instant.parse("2026-07-08T14:00:00Z");
    static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000010002");
    static final UUID SELLER_TERMINAL_ID = UUID.fromString("88000000-0000-0000-0000-000000000001");

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:18.4").asCompatibleSubstituteFor("postgres"))
        .withDatabaseName("tchalanet_db")
        .withUsername("app_user")
        .withPassword("devpass");

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected CommandBus commandBus;

    @Autowired
    protected QueryBus queryBus;

    protected TenantId tenantId;
    protected TchRequestContext tenantAdminContext;
    protected TchRequestContext sellerContext;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.hikari.jdbc-url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    static final class FlywayContainerInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            POSTGRES.start();
            Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .schemas("public", "batch")
                .baselineOnMigrate(true)
                .load()
                .migrate();
        }
    }

    @BeforeEach
    void setUpBusinessRuntimeFixture() {
        var tenantUuid = jdbc.queryForObject(
            "select id from tenant where code = 'tchalanet'",
            UUID.class);
        tenantId = TenantId.of(tenantUuid);
        tenantAdminContext = tenantAdminContext(tenantUuid);
        sellerContext = sellerContext(tenantUuid);
        withContext(tenantAdminContext, () -> {
            upsertSellerTerminal(tenantUuid);
            return null;
        });
    }

    protected <T> T withContext(TchRequestContext context, Supplier<T> work) {
        return TchContextScope.runWithContextResult(context, work);
    }

    private void upsertSellerTerminal(UUID tenantUuid) {
        var now = Timestamp.from(FIXED_NOW);
        jdbc.update(
            """
            insert into seller_terminal (
                id, tenant_id, terminal_code, display_name, status, commission_rate,
                activated_at, created_at, updated_at
            )
            values (?, ?, 'IT-POS-01', 'Integration POS', 'ACTIVE', 15.00, ?, ?, ?)
            on conflict (tenant_id, terminal_code) do update set
                status = excluded.status,
                display_name = excluded.display_name,
                activated_at = excluded.activated_at,
                updated_at = excluded.updated_at
            """,
            SELLER_TERMINAL_ID,
            tenantUuid,
            now,
            now,
            now);
    }

    private static TchRequestContext tenantAdminContext(UUID tenantUuid) {
        return new TchRequestContext(
            "tchalanet",
            tenantUuid,
            "tchalanet",
            tenantUuid,
            ADMIN_USER_ID,
            Set.of(TchRole.TENANT_ADMIN),
            Set.of(),
            Locale.FRENCH,
            "it-admin-request",
            "127.0.0.1",
            "integration-test",
            false,
            null,
            "active",
            ApiScope.TENANT,
            null,
            TenantId.of(tenantUuid),
            ZoneId.of("America/Port-au-Prince"),
            Currency.getInstance(CommonConstants.DEFAULT_CURRENCY),
            null,
            TchActorType.APP_USER,
            null,
            Set.of("TENANT_ADMIN"),
            Set.of("admin.access"),
            ADMIN_USER_ID.toString());
    }

    private static TchRequestContext sellerContext(UUID tenantUuid) {
        return tenantAdminContext(tenantUuid)
            .withResolvedAccess(
                TchActorType.SELLER_TERMINAL,
                SellerTerminalId.of(SELLER_TERMINAL_ID),
                Set.of(),
                Set.of("seller_terminal.sell", "seller_terminal.me.read"))
            .withOperationalContext(
                new OperationalContextHint(
                    OperationalContextSource.SERVER_BOOTSTRAP,
                    OperationalContextTrust.STRONG));
    }

    @TestConfiguration
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock integrationTestClock() {
            return Clock.fixed(FIXED_NOW, ZoneId.of("UTC"));
        }
    }
}
