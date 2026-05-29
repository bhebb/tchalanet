package com.tchalanet.server.core.outlet.internal.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Outlet domain model.
 *
 * <p>Focus: capability checks (canSell / canPayout / canAcceptOfflineSales / salesCapability)
 * and the block/unblock/day lifecycle operations.
 *
 * <p>Guard logic for each operation:
 * <pre>
 *   canSell               = ACTIVE && !outletBlocked && !dayClosed && !salesBlocked
 *   canPayout             = ACTIVE && !outletBlocked && !payoutBlocked
 *   canAcceptOfflineSales = ACTIVE && !outletBlocked && !salesBlocked && !offlineSalesBlocked
 * </pre>
 */
class OutletTest {

    // ── Fixtures ─────────────────────────────────────────────────────────

    private static final TenantId TENANT  = TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));
    private static final OutletId OUTLET  = OutletId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));
    private static final UserId   ACTOR   = UserId.of(UUID.fromString("30000000-0000-0000-0000-000000000001"));
    private static final Instant  NOW     = Instant.parse("2025-06-01T12:00:00Z");

    /** Returns an ACTIVE outlet with all flags cleared — the "happy path" baseline. */
    private static Outlet activeOutlet() {
        return Outlet.createNew(TENANT, "Test Outlet", "test-outlet", OUTLET,
                OutletKind.OWNED_SHOP, null, null)
            .withStatus(OutletStatus.ACTIVE);
    }

    // ── canSell ──────────────────────────────────────────────────────────

    @Nested
    class CanSell {

        @Test
        void allowedWhenActiveAndAllFlagsClear() {
            assertThat(activeOutlet().canSell()).isTrue();
        }

        @Test
        void blockedWhenStatusIsDraft() {
            var outlet = Outlet.createNew(TENANT, "x", "x", OUTLET, OutletKind.OWNED_SHOP, null, null);
            assertThat(outlet.status()).isEqualTo(OutletStatus.DRAFT);
            assertThat(outlet.canSell()).isFalse();
        }

        @Test
        void blockedWhenStatusIsSuspended() {
            assertThat(activeOutlet().withStatus(OutletStatus.SUSPENDED).canSell()).isFalse();
        }

        @Test
        void blockedWhenStatusIsClosed() {
            assertThat(activeOutlet().withStatus(OutletStatus.CLOSED).canSell()).isFalse();
        }

        @Test
        void blockedWhenOutletIsGloballyBlocked() {
            var outlet = activeOutlet().blockOutlet("emergency", NOW, ACTOR);
            assertThat(outlet.canSell()).isFalse();
        }

        @Test
        void blockedWhenDayClosed() {
            var outlet = activeOutlet().closeDay();
            assertThat(outlet.canSell()).isFalse();
        }

        @Test
        void blockedWhenSalesBlocked() {
            var outlet = activeOutlet().blockSales("audit", NOW, ACTOR);
            assertThat(outlet.canSell()).isFalse();
        }

        @Test
        void outletBlockTakesPriorityOverSalesBlock() {
            // Both outlet-blocked and sales-blocked: still false (outlet guard fires first)
            var outlet = activeOutlet()
                .blockOutlet("global", NOW, ACTOR)
                .blockSales("sales", NOW, ACTOR);
            assertThat(outlet.canSell()).isFalse();
        }

        @Test
        void outletBlockTakesPriorityOverDayClosed() {
            var outlet = activeOutlet()
                .blockOutlet("global", NOW, ACTOR)
                .closeDay();
            assertThat(outlet.canSell()).isFalse();
        }

        @Test
        void payoutBlockDoesNotAffectSell() {
            var outlet = activeOutlet().blockPayout("payout_issue", NOW, ACTOR);
            assertThat(outlet.canSell()).isTrue();
        }

        @Test
        void offlineSalesBlockDoesNotAffectSell() {
            var outlet = activeOutlet().blockOfflineSales("offline_issue", NOW, ACTOR);
            assertThat(outlet.canSell()).isTrue();
        }

        @Test
        void unblockingOutletRestoresSell() {
            var outlet = activeOutlet().blockOutlet("temp", NOW, ACTOR).unblockOutlet();
            assertThat(outlet.canSell()).isTrue();
        }

        @Test
        void reopeningDayRestoresSell() {
            var outlet = activeOutlet().closeDay().reopenDay();
            assertThat(outlet.canSell()).isTrue();
        }

        @Test
        void unblockingSalesRestoresSell() {
            var outlet = activeOutlet().blockSales("temp", NOW, ACTOR).unblockSales();
            assertThat(outlet.canSell()).isTrue();
        }
    }

    // ── canPayout ────────────────────────────────────────────────────────

    @Nested
    class CanPayout {

        @Test
        void allowedWhenActiveAndAllFlagsClear() {
            assertThat(activeOutlet().canPayout()).isTrue();
        }

        @Test
        void blockedWhenStatusIsDraft() {
            var outlet = Outlet.createNew(TENANT, "x", "x", OUTLET, OutletKind.OWNED_SHOP, null, null);
            assertThat(outlet.canPayout()).isFalse();
        }

        @Test
        void blockedWhenOutletIsGloballyBlocked() {
            var outlet = activeOutlet().blockOutlet("emergency", NOW, ACTOR);
            assertThat(outlet.canPayout()).isFalse();
        }

        @Test
        void blockedWhenPayoutBlocked() {
            var outlet = activeOutlet().blockPayout("fraud", NOW, ACTOR);
            assertThat(outlet.canPayout()).isFalse();
        }

        @Test
        void dayClosedDoesNotBlockPayout() {
            // day_closed only blocks sales, NOT payout
            var outlet = activeOutlet().closeDay();
            assertThat(outlet.canPayout()).isTrue();
        }

        @Test
        void salesBlockDoesNotBlockPayout() {
            // sales block is independent from payout block
            var outlet = activeOutlet().blockSales("audit", NOW, ACTOR);
            assertThat(outlet.canPayout()).isTrue();
        }

        @Test
        void offlineSalesBlockDoesNotBlockPayout() {
            var outlet = activeOutlet().blockOfflineSales("offline_issue", NOW, ACTOR);
            assertThat(outlet.canPayout()).isTrue();
        }

        @Test
        void outletBlockOverridesPayoutBlock() {
            var outlet = activeOutlet()
                .blockOutlet("global", NOW, ACTOR)
                .blockPayout("payout", NOW, ACTOR);
            assertThat(outlet.canPayout()).isFalse();
        }

        @Test
        void unblockingPayoutRestoresPayout() {
            var outlet = activeOutlet().blockPayout("temp", NOW, ACTOR).unblockPayout();
            assertThat(outlet.canPayout()).isTrue();
        }
    }

    // ── canAcceptOfflineSales ────────────────────────────────────────────

    @Nested
    class CanAcceptOfflineSales {

        @Test
        void allowedWhenActiveAndAllFlagsClear() {
            assertThat(activeOutlet().canAcceptOfflineSales()).isTrue();
        }

        @Test
        void blockedWhenStatusIsDraft() {
            var outlet = Outlet.createNew(TENANT, "x", "x", OUTLET, OutletKind.OWNED_SHOP, null, null);
            assertThat(outlet.canAcceptOfflineSales()).isFalse();
        }

        @Test
        void blockedWhenOutletIsGloballyBlocked() {
            var outlet = activeOutlet().blockOutlet("emergency", NOW, ACTOR);
            assertThat(outlet.canAcceptOfflineSales()).isFalse();
        }

        @Test
        void blockedWhenSalesBlocked() {
            // salesBlock also prevents offline sales grant
            var outlet = activeOutlet().blockSales("audit", NOW, ACTOR);
            assertThat(outlet.canAcceptOfflineSales()).isFalse();
        }

        @Test
        void blockedWhenOfflineSalesBlocked() {
            var outlet = activeOutlet().blockOfflineSales("offline_disabled", NOW, ACTOR);
            assertThat(outlet.canAcceptOfflineSales()).isFalse();
        }

        @Test
        void dayClosedDoesNotBlockOfflineSales() {
            // day_closed only blocks canSell, NOT offline grant
            var outlet = activeOutlet().closeDay();
            assertThat(outlet.canAcceptOfflineSales()).isTrue();
        }

        @Test
        void payoutBlockDoesNotBlockOfflineSales() {
            var outlet = activeOutlet().blockPayout("fraud", NOW, ACTOR);
            assertThat(outlet.canAcceptOfflineSales()).isTrue();
        }

        @Test
        void outletBlockOverridesAllOtherFlags() {
            var outlet = activeOutlet()
                .blockOutlet("global", NOW, ACTOR)
                .blockSales("sales", NOW, ACTOR)
                .blockOfflineSales("offline", NOW, ACTOR);
            assertThat(outlet.canAcceptOfflineSales()).isFalse();
        }

        @Test
        void unblockingOfflineSalesRestoresGrant() {
            var outlet = activeOutlet()
                .blockOfflineSales("temp", NOW, ACTOR)
                .unblockOfflineSales();
            assertThat(outlet.canAcceptOfflineSales()).isTrue();
        }

        @Test
        void unblockingSalesAlsoRestoresOfflineWhenOnlyThatWasBlocking() {
            var outlet = activeOutlet().blockSales("temp", NOW, ACTOR).unblockSales();
            assertThat(outlet.canAcceptOfflineSales()).isTrue();
        }
    }

    // ── salesCapability ──────────────────────────────────────────────────

    @Nested
    class SalesCapabilityCheck {

        @Test
        void allowedWhenFullyActive() {
            assertThat(activeOutlet().salesCapability().allowed()).isTrue();
        }

        @Test
        void blockedWithOutletNotActiveWhenDraft() {
            var cap = Outlet.createNew(TENANT, "x", "x", OUTLET, OutletKind.OWNED_SHOP, null, null)
                .salesCapability();
            assertThat(cap.allowed()).isFalse();
            assertThat(cap.reason()).isEqualTo("OUTLET_NOT_ACTIVE");
        }

        @Test
        void blockedWithOutletBlockedReasonWhenGloballyBlocked() {
            var cap = activeOutlet().blockOutlet("fraud_check", NOW, ACTOR).salesCapability();
            assertThat(cap.allowed()).isFalse();
            assertThat(cap.reason()).isEqualTo("fraud_check");
        }

        @Test
        void blockedWithDefaultOutletBlockedReasonWhenNoReasonProvided() {
            var cap = activeOutlet().blockOutlet(null, NOW, ACTOR).salesCapability();
            assertThat(cap.allowed()).isFalse();
            assertThat(cap.reason()).isEqualTo("OUTLET_BLOCKED");
        }

        @Test
        void blockedWithDayClosedWhenDayClosed() {
            var cap = activeOutlet().closeDay().salesCapability();
            assertThat(cap.allowed()).isFalse();
            assertThat(cap.reason()).isEqualTo("DAY_CLOSED");
        }

        @Test
        void blockedWithSalesBlockedReasonWhenSalesBlocked() {
            var cap = activeOutlet().blockSales("compliance", NOW, ACTOR).salesCapability();
            assertThat(cap.allowed()).isFalse();
            assertThat(cap.reason()).isEqualTo("compliance");
        }

        @Test
        void outletBlockTakesPriorityOverDayClosed() {
            var cap = activeOutlet()
                .blockOutlet("global", NOW, ACTOR)
                .closeDay()
                .salesCapability();
            assertThat(cap.reason()).isEqualTo("global");
        }

        @Test
        void outletBlockTakesPriorityOverSalesBlock() {
            var cap = activeOutlet()
                .blockOutlet("global", NOW, ACTOR)
                .blockSales("sales", NOW, ACTOR)
                .salesCapability();
            assertThat(cap.reason()).isEqualTo("global");
        }

        @Test
        void dayClosedTakesPriorityOverSalesBlock() {
            var cap = activeOutlet()
                .closeDay()
                .blockSales("sales", NOW, ACTOR)
                .salesCapability();
            assertThat(cap.reason()).isEqualTo("DAY_CLOSED");
        }
    }

    // ── Day close / reopen ───────────────────────────────────────────────

    @Nested
    class DayLifecycle {

        @Test
        void closeDaySetsDayClosed() {
            var outlet = activeOutlet().closeDay();
            assertThat(outlet.dayClosed()).isTrue();
        }

        @Test
        void closeDayIsIdempotent() {
            var outlet = activeOutlet().closeDay();
            assertThat(outlet.closeDay()).isSameAs(outlet);
        }

        @Test
        void reopenDayClearsDayClosed() {
            var outlet = activeOutlet().closeDay().reopenDay();
            assertThat(outlet.dayClosed()).isFalse();
        }

        @Test
        void reopenDayIsIdempotent() {
            var outlet = activeOutlet(); // day already open
            assertThat(outlet.reopenDay()).isSameAs(outlet);
        }
    }

    // ── Outlet-level block / unblock ─────────────────────────────────────

    @Nested
    class OutletBlockLifecycle {

        @Test
        void blockOutletSetsBlockState() {
            var outlet = activeOutlet().blockOutlet("compliance", NOW, ACTOR);
            assertThat(outlet.outletBlock().blocked()).isTrue();
            assertThat(outlet.outletBlock().reason()).isEqualTo("compliance");
            assertThat(outlet.outletBlock().at()).isEqualTo(NOW);
            assertThat(outlet.outletBlock().by()).isEqualTo(ACTOR);
        }

        @Test
        void unblockOutletClearsBlockState() {
            var outlet = activeOutlet().blockOutlet("compliance", NOW, ACTOR).unblockOutlet();
            assertThat(outlet.outletBlock().blocked()).isFalse();
            assertThat(outlet.outletBlock().reason()).isNull();
        }

        @Test
        void unblockOutletIsIdempotentWhenNotBlocked() {
            var outlet = activeOutlet(); // already unblocked
            assertThat(outlet.unblockOutlet()).isSameAs(outlet);
        }

        @Test
        void blockOutletDoesNotAffectOtherBlockStates() {
            var outlet = activeOutlet().blockOutlet("global", NOW, ACTOR);
            // Other block states remain none
            assertThat(outlet.salesBlock().blocked()).isFalse();
            assertThat(outlet.payoutBlock().blocked()).isFalse();
            assertThat(outlet.offlineSalesBlock().blocked()).isFalse();
        }
    }

    // ── Independent block states ─────────────────────────────────────────

    @Nested
    class BlockStateIndependence {

        @Test
        void eachBlockFlagIsIndependent() {
            var outlet = activeOutlet()
                .blockSales("s", NOW, ACTOR)
                .blockPayout("p", NOW, ACTOR)
                .blockOfflineSales("o", NOW, ACTOR);

            assertThat(outlet.salesBlock().blocked()).isTrue();
            assertThat(outlet.payoutBlock().blocked()).isTrue();
            assertThat(outlet.offlineSalesBlock().blocked()).isTrue();
            assertThat(outlet.outletBlock().blocked()).isFalse(); // global untouched
        }

        @Test
        void unblockingSalesDoesNotAffectPayout() {
            var outlet = activeOutlet()
                .blockSales("s", NOW, ACTOR)
                .blockPayout("p", NOW, ACTOR)
                .unblockSales();

            assertThat(outlet.salesBlock().blocked()).isFalse();
            assertThat(outlet.payoutBlock().blocked()).isTrue();
        }

        @Test
        void unblockingPayoutDoesNotAffectSales() {
            var outlet = activeOutlet()
                .blockPayout("p", NOW, ACTOR)
                .blockSales("s", NOW, ACTOR)
                .unblockPayout();

            assertThat(outlet.payoutBlock().blocked()).isFalse();
            assertThat(outlet.salesBlock().blocked()).isTrue();
        }
    }
}
