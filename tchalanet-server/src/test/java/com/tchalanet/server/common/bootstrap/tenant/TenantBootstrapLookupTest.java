package com.tchalanet.server.common.bootstrap.tenant;

import com.tchalanet.server.common.types.id.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("TenantBootstrapLookup")
class TenantBootstrapLookupTest {

    private JdbcTemplate jdbcTemplate;
    private TenantBootstrapLookup lookup;

    @BeforeEach
    void setUp() {
        DataSource dataSource = mock(DataSource.class);
        jdbcTemplate = mock(JdbcTemplate.class);

        // Use reflection to inject mock JdbcTemplate
        lookup = new TenantBootstrapLookup(dataSource);
        try {
            var field = TenantBootstrapLookup.class.getDeclaredField("jdbc");
            field.setAccessible(true);
            field.set(lookup, jdbcTemplate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock JdbcTemplate", e);
        }
    }

    @Nested
    @DisplayName("findTenantUuidByCode")
    class FindTenantUuidByCode {

        @Test
        @DisplayName("should return UUID when tenant code exists")
        void shouldReturnUuidWhenTenantCodeExists() {
            // given
            String code = "demo";
            UUID expectedUuid = UUID.randomUUID();

            when(jdbcTemplate.query(
                anyString(),
                any(PreparedStatementSetter.class),
                any(ResultSetExtractor.class)
            )).thenAnswer(invocation -> {
                // Simulate ResultSetExtractor returning UUID
                return expectedUuid;
            });

            // when
            Optional<UUID> result = lookup.findTenantUuidByCode(code);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(expectedUuid);
        }

        @Test
        @DisplayName("should return empty when tenant code not found")
        void shouldReturnEmptyWhenTenantCodeNotFound() {
            // given
            String code = "nonexistent";

            when(jdbcTemplate.query(
                anyString(),
                any(PreparedStatementSetter.class),
                any(ResultSetExtractor.class)
            )).thenReturn(null);

            // when
            Optional<UUID> result = lookup.findTenantUuidByCode(code);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when code is null")
        void shouldReturnEmptyWhenCodeIsNull() {
            // when
            Optional<UUID> result = lookup.findTenantUuidByCode(null);

            // then
            assertThat(result).isEmpty();
            verifyNoInteractions(jdbcTemplate);
        }

        @Test
        @DisplayName("should return empty when code is blank")
        void shouldReturnEmptyWhenCodeIsBlank() {
            // when
            Optional<UUID> result = lookup.findTenantUuidByCode("  ");

            // then
            assertThat(result).isEmpty();
            verifyNoInteractions(jdbcTemplate);
        }

        @Test
        @DisplayName("should return empty when query throws exception")
        void shouldReturnEmptyWhenQueryThrowsException() {
            // given
            String code = "demo";

            when(jdbcTemplate.query(
                anyString(),
                any(PreparedStatementSetter.class),
                any(ResultSetExtractor.class)
            )).thenThrow(new RuntimeException("Database error"));

            // when
            Optional<UUID> result = lookup.findTenantUuidByCode(code);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findTenantInfoByCode")
    class FindTenantInfoByCode {

        @Test
        @DisplayName("should return tenant info when tenant code exists")
        void shouldReturnTenantInfoWhenTenantCodeExists() throws Exception {
            // given
            String code = "demo";
            UUID tenantUuid = UUID.randomUUID();
            String timezone = "Europe/Paris";
            String currencyCode = "EUR";

            // Mock ResultSet
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(true);
            when(rs.getObject(1)).thenReturn(tenantUuid);
            when(rs.getString(2)).thenReturn(timezone);
            when(rs.getString(3)).thenReturn(currencyCode);

            when(jdbcTemplate.query(
                anyString(),
                any(PreparedStatementSetter.class),
                any(ResultSetExtractor.class)
            )).thenAnswer(invocation -> {
                ResultSetExtractor<?> extractor = invocation.getArgument(2);
                return extractor.extractData(rs);
            });

            // when
            Optional<TenantBootstrapInfo> result = lookup.findTenantInfoByCode(code);

            // then
            assertThat(result).isPresent();
            TenantBootstrapInfo info = result.get();
            assertThat(info.tenantId()).isEqualTo(TenantId.of(tenantUuid));
            assertThat(info.tenantZoneId()).isEqualTo(ZoneId.of(timezone));
            assertThat(info.currency()).isEqualTo(Currency.getInstance(currencyCode));
        }

        @Test
        @DisplayName("should use UTC fallback when timezone is null")
        void shouldUseUtcFallbackWhenTimezoneIsNull() throws Exception {
            // given
            String code = "demo";
            UUID tenantUuid = UUID.randomUUID();

            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(true);
            when(rs.getObject(1)).thenReturn(tenantUuid);
            when(rs.getString(2)).thenReturn(null);
            when(rs.getString(3)).thenReturn("USD");

            when(jdbcTemplate.query(
                anyString(),
                any(PreparedStatementSetter.class),
                any(ResultSetExtractor.class)
            )).thenAnswer(invocation -> {
                ResultSetExtractor<?> extractor = invocation.getArgument(2);
                return extractor.extractData(rs);
            });

            // when
            Optional<TenantBootstrapInfo> result = lookup.findTenantInfoByCode(code);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().tenantZoneId()).isEqualTo(ZoneId.of("UTC"));
        }

        @Test
        @DisplayName("should use USD fallback when currency is null")
        void shouldUseUsdFallbackWhenCurrencyIsNull() throws Exception {
            // given
            String code = "demo";
            UUID tenantUuid = UUID.randomUUID();

            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(true);
            when(rs.getObject(1)).thenReturn(tenantUuid);
            when(rs.getString(2)).thenReturn("UTC");
            when(rs.getString(3)).thenReturn(null);

            when(jdbcTemplate.query(
                anyString(),
                any(PreparedStatementSetter.class),
                any(ResultSetExtractor.class)
            )).thenAnswer(invocation -> {
                ResultSetExtractor<?> extractor = invocation.getArgument(2);
                return extractor.extractData(rs);
            });

            // when
            Optional<TenantBootstrapInfo> result = lookup.findTenantInfoByCode(code);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().currency()).isEqualTo(Currency.getInstance("USD"));
        }

        @Test
        @DisplayName("should return empty when tenant code not found")
        void shouldReturnEmptyWhenTenantCodeNotFound() throws Exception {
            // given
            String code = "nonexistent";

            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(false);

            when(jdbcTemplate.query(
                anyString(),
                any(PreparedStatementSetter.class),
                any(ResultSetExtractor.class)
            )).thenAnswer(invocation -> {
                ResultSetExtractor<?> extractor = invocation.getArgument(2);
                return extractor.extractData(rs);
            });

            // when
            Optional<TenantBootstrapInfo> result = lookup.findTenantInfoByCode(code);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when tenant code is null")
        void shouldReturnEmptyWhenTenantCodeIsNull() {
            // when
            Optional<TenantBootstrapInfo> result = lookup.findTenantInfoByCode(null);

            // then
            assertThat(result).isEmpty();
            verifyNoInteractions(jdbcTemplate);
        }

        @Test
        @DisplayName("should return empty when query throws exception")
        void shouldReturnEmptyWhenQueryThrowsException() {
            // given
            String code = "demo";

            when(jdbcTemplate.query(
                anyString(),
                any(PreparedStatementSetter.class),
                any(ResultSetExtractor.class)
            )).thenThrow(new RuntimeException("Database error"));

            // when
            Optional<TenantBootstrapInfo> result = lookup.findTenantInfoByCode(code);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findTenantInfoById")
    class FindTenantInfoById {

        @Test
        @DisplayName("should return tenant info when tenant ID exists")
        void shouldReturnTenantInfoWhenTenantIdExists() throws Exception {
            // given
            UUID tenantUuid = UUID.randomUUID();
            TenantId tenantId = TenantId.of(tenantUuid);
            String timezone = "Asia/Tokyo";
            String currencyCode = "JPY";

            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(true);
            when(rs.getObject(1)).thenReturn(tenantUuid);
            when(rs.getString(2)).thenReturn(timezone);
            when(rs.getString(3)).thenReturn(currencyCode);

            when(jdbcTemplate.query(
                anyString(),
                any(PreparedStatementSetter.class),
                any(ResultSetExtractor.class)
            )).thenAnswer(invocation -> {
                ResultSetExtractor<?> extractor = invocation.getArgument(2);
                return extractor.extractData(rs);
            });

            // when
            Optional<TenantBootstrapInfo> result = lookup.findTenantInfoById(tenantId);

            // then
            assertThat(result).isPresent();
            TenantBootstrapInfo info = result.get();
            assertThat(info.tenantId()).isEqualTo(tenantId);
            assertThat(info.tenantZoneId()).isEqualTo(ZoneId.of(timezone));
            assertThat(info.currency()).isEqualTo(Currency.getInstance(currencyCode));
        }

        @Test
        @DisplayName("should return empty when tenant ID not found")
        void shouldReturnEmptyWhenTenantIdNotFound() throws Exception {
            // given
            TenantId tenantId = TenantId.of(UUID.randomUUID());

            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(false);

            when(jdbcTemplate.query(
                anyString(),
                any(PreparedStatementSetter.class),
                any(ResultSetExtractor.class)
            )).thenAnswer(invocation -> {
                ResultSetExtractor<?> extractor = invocation.getArgument(2);
                return extractor.extractData(rs);
            });

            // when
            Optional<TenantBootstrapInfo> result = lookup.findTenantInfoById(tenantId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when tenant ID is null")
        void shouldReturnEmptyWhenTenantIdIsNull() {
            // when
            Optional<TenantBootstrapInfo> result = lookup.findTenantInfoById(null);

            // then
            assertThat(result).isEmpty();
            verifyNoInteractions(jdbcTemplate);
        }

        @Test
        @DisplayName("should return empty when query throws exception")
        void shouldReturnEmptyWhenQueryThrowsException() {
            // given
            TenantId tenantId = TenantId.of(UUID.randomUUID());

            when(jdbcTemplate.query(
                anyString(),
                any(PreparedStatementSetter.class),
                any(ResultSetExtractor.class)
            )).thenThrow(new RuntimeException("Database error"));

            // when
            Optional<TenantBootstrapInfo> result = lookup.findTenantInfoById(tenantId);

            // then
            assertThat(result).isEmpty();
        }
    }
}
