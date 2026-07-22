package com.tchalanet.server.integration.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.core.drawresult.api.command.ConfirmDrawResultCommand;
import com.tchalanet.server.core.drawresult.api.command.OverrideDrawResultCommand;
import com.tchalanet.server.core.drawresult.api.command.RecordManualDrawResultCommand;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Draw result audit fields (Spring integration)")
class DrawResultAuditSpringIntegrationTest extends BusinessRuntimeIntegrationTestBase {

  private static final LocalDate MANUAL_DATE = LocalDate.of(2026, 7, 15);
  private static final LocalDate REVIEW_DATE = LocalDate.of(2026, 7, 16);
  private static final LocalDate OVERRIDE_DATE = LocalDate.of(2026, 7, 17);

  @Test
  @DisplayName("seeded non-automatic providers are active manual result slots")
  void nonAutomaticProviderSeedsAreManualSlots() {
    var manualSlots =
        jdbc.queryForList(
            """
                select provider, slot_key, active, source_cfg->>'source_mode' as source_mode
                  from result_slot
                 where provider in ('TN', 'IL', 'MO', 'MN')
                 order by provider, slot_key
                """);

    assertThat(manualSlots).hasSize(7);
    assertThat(manualSlots)
        .allSatisfy(
            row -> {
              assertThat(row.get("active")).isEqualTo(true);
              assertThat(row.get("source_mode")).isEqualTo("MANUAL");
            });

    var ohioModes =
        jdbc.queryForList(
            """
                select slot_key, active, source_cfg->>'source_mode' as source_mode
                  from result_slot
                 where provider = 'OH'
                 order by slot_key
                """);

    assertThat(ohioModes).hasSize(2);
    assertThat(ohioModes)
        .allSatisfy(
            row -> {
              assertThat(row.get("active")).isEqualTo(true);
              assertThat(row.get("source_mode")).isNull();
            });
  }

  @Test
  @DisplayName("manual result create, provisional confirm, and override update audit fields")
  void writePathsPopulateAuditFields() {
    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(
                new RecordManualDrawResultCommand(
                    tenantId,
                    MANUAL_DATE,
                    "NY_MID",
                    "audit-manual",
                    "manual audit test",
                    "123",
                    "1234",
                    false,
                    null,
                    false)));

    var manual = drawResult("NY_MID", MANUAL_DATE);
    assertThat(manual.get("status")).isEqualTo("CONFIRMED");
    assertThat(manual.get("source")).isEqualTo("MANUAL");
    assertThat(manual.get("quality")).isEqualTo("COMPLETE");
    assertThat(manual.get("fetched_at")).isNotNull();
    assertThat(manual.get("source_result")).isNotNull();
    assertThat(manual.get("haiti_result")).isNotNull();
    assertThat(manual.get("raw_payload")).isNotNull();
    assertThat(manual.get("flags")).isNotNull();
    assertThat(manual.get("override_reason")).isNull();
    assertThat(manual.get("created_by")).isEqualTo(ADMIN_USER_ID);
    assertThat(manual.get("updated_by")).isEqualTo(ADMIN_USER_ID);
    assertThat(manual.get("created_at")).isNotNull();
    assertThat(manual.get("updated_at")).isNotNull();

    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(
                new RecordManualDrawResultCommand(
                    tenantId,
                    REVIEW_DATE,
                    "NY_EVE",
                    "audit-propose",
                    "provisional audit test",
                    "456",
                    "4567",
                    false,
                    null,
                    true)));

    var provisional = drawResult("NY_EVE", REVIEW_DATE);
    assertThat(provisional.get("status")).isEqualTo("PROVISIONAL");
    assertThat(provisional.get("source")).isEqualTo("MANUAL");
    assertThat(provisional.get("quality")).isEqualTo("COMPLETE");
    var provisionalId = (UUID) provisional.get("id");
    var provisionalUpdatedAt = (Timestamp) provisional.get("updated_at");

    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(
                new ConfirmDrawResultCommand(
                    DrawResultId.of(provisionalId), tenantAdminContext.externalSubject())));

    var confirmed = drawResult("NY_EVE", REVIEW_DATE);
    assertThat(confirmed.get("status")).isEqualTo("CONFIRMED");
    assertThat(confirmed.get("source")).isEqualTo("MANUAL");
    assertThat(confirmed.get("quality")).isEqualTo("COMPLETE");
    assertThat(confirmed.get("created_by")).isEqualTo(ADMIN_USER_ID);
    assertThat(confirmed.get("updated_by")).isEqualTo(ADMIN_USER_ID);
    assertThat((Timestamp) confirmed.get("updated_at")).isNotEqualTo(provisionalUpdatedAt);
    assertThat(auditUpdateCount(provisionalId)).isGreaterThanOrEqualTo(1);

    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(
                new RecordManualDrawResultCommand(
                    tenantId,
                    OVERRIDE_DATE,
                    "FL_MID",
                    "audit-override-base",
                    "override base",
                    "789",
                    "7890",
                    false,
                    null,
                    false)));
    var beforeOverride = drawResult("FL_MID", OVERRIDE_DATE);
    var overrideId = (UUID) beforeOverride.get("id");
    var beforeOverrideUpdatedAt = (Timestamp) beforeOverride.get("updated_at");

    withContext(
        tenantAdminContext,
        () ->
            commandBus.execute(
                new OverrideDrawResultCommand(
                    tenantId, "FL_MID", OVERRIDE_DATE, "321", "4321", "audit override", true)));

    var overridden = drawResult("FL_MID", OVERRIDE_DATE);
    assertThat(overridden.get("id")).isEqualTo(overrideId);
    assertThat(overridden.get("status")).isEqualTo("OVERRIDDEN");
    assertThat(overridden.get("source")).isEqualTo("ADMIN_OVERRIDE");
    assertThat(overridden.get("quality")).isEqualTo("COMPLETE");
    assertThat(overridden.get("source_result")).isNotNull();
    assertThat(overridden.get("haiti_result")).isNotNull();
    assertThat(overridden.get("raw_payload")).isNotNull();
    assertThat(overridden.get("flags")).isNotNull();
    assertThat(overridden.get("created_by")).isEqualTo(ADMIN_USER_ID);
    assertThat(overridden.get("updated_by")).isEqualTo(ADMIN_USER_ID);
    assertThat(overridden.get("override_reason")).isEqualTo("audit override");
    assertThat((Timestamp) overridden.get("updated_at")).isNotEqualTo(beforeOverrideUpdatedAt);
    assertThat(auditUpdateCount(overrideId)).isGreaterThanOrEqualTo(1);
  }

  private Map<String, Object> drawResult(String slotKey, LocalDate resultDate) {
    return jdbc.queryForMap(
        """
            select dr.id,
                   dr.status,
                   dr.source,
                   dr.quality,
                   dr.override_reason,
                   dr.fetched_at,
                   dr.source_result,
                   dr.haiti_result,
                   dr.raw_payload,
                   dr.flags,
                   dr.created_at,
                   dr.created_by,
                   dr.updated_at,
                   dr.updated_by
              from draw_result dr
              join result_slot rs on rs.id = dr.result_slot_id
             where rs.slot_key = ?
               and dr.result_date = ?
            """,
        slotKey,
        java.sql.Date.valueOf(resultDate));
  }

  private Integer auditUpdateCount(UUID drawResultId) {
    return jdbc.queryForObject(
        "select count(*) from draw_result_aud where id = ? and revtype = 1",
        Integer.class,
        drawResultId);
  }
}
