package com.tchalanet.server.platform.tenantgame.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.catalog.game.api.model.GameView;
import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.platform.tenantgame.api.model.request.UpdateTenantGameSettingsRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantGameConfigValidatorTest {

  private final TenantGameConfigValidator validator = new TenantGameConfigValidator();

  private GameView activeGame() {
    return new GameView(
        GameId.of(UUID.randomUUID()),
        "BORLETTE",
        "Borlette",
        "LOTO",
        "2",
        2,
        2,
        "desc",
        true,
        0,
        Instant.now(),
        Instant.now());
  }

  private GameView inactiveGame() {
    return new GameView(
        GameId.of(UUID.randomUUID()),
        "OLD",
        "Old Game",
        "LOTO",
        "2",
        2,
        2,
        "desc",
        false,
        0,
        Instant.now(),
        Instant.now());
  }

  // ── validateEnableGame ─────────────────────────────────────────────────

  @Test
  void activeGameCanBeEnabled() {
    assertThatNoException().isThrownBy(() -> validator.validateEnableGame(activeGame()));
  }

  @Test
  void inactiveGameCannotBeEnabled() {
    assertThatThrownBy(() -> validator.validateEnableGame(inactiveGame()))
        .isInstanceOf(ProblemRestException.class)
        .satisfies(error -> assertCode(error, "tenantgame.catalog_game_inactive"));
  }

  // ── validateSettings — displayName ────────────────────────────────────

  @Test
  void displayNameOver128CharsIsRejected() {
    var req = UpdateTenantGameSettingsRequest.builder().displayName("A".repeat(129)).build();
    assertThatThrownBy(() -> validator.validateSettings(req))
        .isInstanceOf(ProblemRestException.class)
        .satisfies(error -> assertCode(error, "tenantgame.display_name_too_long"));
  }

  @Test
  void displayNameOf128CharsIsAllowed() {
    var req = UpdateTenantGameSettingsRequest.builder().displayName("A".repeat(128)).build();
    assertThatNoException().isThrownBy(() -> validator.validateSettings(req));
  }

  @Test
  void nullDisplayNameIsAllowed() {
    assertThatNoException()
        .isThrownBy(
            () -> validator.validateSettings(UpdateTenantGameSettingsRequest.builder().build()));
  }

  // ── validateSettings — displayOrder ───────────────────────────────────

  @Test
  void negativeDisplayOrderIsRejected() {
    var req = UpdateTenantGameSettingsRequest.builder().displayOrder(-1).build();
    assertThatThrownBy(() -> validator.validateSettings(req))
        .isInstanceOf(ProblemRestException.class)
        .satisfies(error -> assertCode(error, "tenantgame.display_order_invalid"));
  }

  @Test
  void zeroDisplayOrderIsAllowed() {
    var req = UpdateTenantGameSettingsRequest.builder().displayOrder(0).build();
    assertThatNoException().isThrownBy(() -> validator.validateSettings(req));
  }

  // ── validateSettings — stakes ─────────────────────────────────────────

  @Test
  void negativeMinStakeIsRejected() {
    var req = UpdateTenantGameSettingsRequest.builder().minStake(BigDecimal.valueOf(-1)).build();
    assertThatThrownBy(() -> validator.validateSettings(req))
        .isInstanceOf(ProblemRestException.class)
        .satisfies(error -> assertCode(error, "tenantgame.min_stake_invalid"));
  }

  @Test
  void minGreaterThanMaxIsRejected() {
    var req =
        UpdateTenantGameSettingsRequest.builder()
            .minStake(BigDecimal.valueOf(100))
            .maxStake(BigDecimal.valueOf(50))
            .build();
    assertThatThrownBy(() -> validator.validateSettings(req))
        .isInstanceOf(ProblemRestException.class)
        .satisfies(error -> assertCode(error, "tenantgame.stake_range_invalid"));
  }

  @Test
  void equalMinAndMaxIsAllowed() {
    var req =
        UpdateTenantGameSettingsRequest.builder()
            .minStake(BigDecimal.valueOf(10))
            .maxStake(BigDecimal.valueOf(10))
            .build();
    assertThatNoException().isThrownBy(() -> validator.validateSettings(req));
  }

  @Test
  void validStakeRangeIsAllowed() {
    var req =
        UpdateTenantGameSettingsRequest.builder()
            .minStake(BigDecimal.valueOf(10))
            .maxStake(BigDecimal.valueOf(500))
            .build();
    assertThatNoException().isThrownBy(() -> validator.validateSettings(req));
  }

  @Test
  void effectiveMinGreaterThanExistingMaxIsRejected() {
    assertThatThrownBy(
            () -> validator.validateStakeRange(BigDecimal.valueOf(100), BigDecimal.valueOf(50)))
        .isInstanceOf(ProblemRestException.class)
        .satisfies(error -> assertCode(error, "tenantgame.stake_range_invalid"));
  }

  // ── validateSettings — availabilityDays ──────────────────────────────

  @Test
  void validDayCodesAreAllowed() {
    var req = UpdateTenantGameSettingsRequest.builder().availabilityDays("MON,WED,FRI").build();
    assertThatNoException().isThrownBy(() -> validator.validateSettings(req));
  }

  @Test
  void invalidDayCodeIsRejected() {
    var req = UpdateTenantGameSettingsRequest.builder().availabilityDays("MON,MONDAY").build();
    assertThatThrownBy(() -> validator.validateSettings(req))
        .isInstanceOf(ProblemRestException.class)
        .satisfies(error -> assertCode(error, "tenantgame.availability_day_invalid"));
  }

  @Test
  void allSevenDaysAreAllowed() {
    var req =
        UpdateTenantGameSettingsRequest.builder()
            .availabilityDays("MON,TUE,WED,THU,FRI,SAT,SUN")
            .build();
    assertThatNoException().isThrownBy(() -> validator.validateSettings(req));
  }

  // ── validateSettings — time ───────────────────────────────────────────

  @Test
  void validTimeFormatIsAllowed() {
    var req =
        UpdateTenantGameSettingsRequest.builder()
            .startLocalTime("08:00")
            .endLocalTime("22:00")
            .build();
    assertThatNoException().isThrownBy(() -> validator.validateSettings(req));
  }

  @Test
  void invalidTimeFormatIsRejected() {
    var req = UpdateTenantGameSettingsRequest.builder().startLocalTime("8am").build();
    assertThatThrownBy(() -> validator.validateSettings(req))
        .isInstanceOf(ProblemRestException.class)
        .satisfies(error -> assertCode(error, "tenantgame.time_invalid"));
  }

  @Test
  void nullTimesAreAllowed() {
    var req = UpdateTenantGameSettingsRequest.builder().build();
    assertThatNoException().isThrownBy(() -> validator.validateSettings(req));
  }

  private static void assertCode(Throwable throwable, String code) {
    assertThat(((ProblemRestException) throwable).getProblem().getProperties())
        .containsEntry("code", code);
  }
}
