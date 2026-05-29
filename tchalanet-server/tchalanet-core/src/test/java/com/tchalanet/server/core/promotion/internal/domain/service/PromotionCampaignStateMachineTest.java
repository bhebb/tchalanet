package com.tchalanet.server.core.promotion.internal.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignStatus;
import com.tchalanet.server.core.promotion.internal.domain.model.PromotionCampaignTransition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PromotionCampaignStateMachine")
class PromotionCampaignStateMachineTest {

    private final PromotionCampaignStateMachine sm = new PromotionCampaignStateMachine();

    // ── ACTIVATE ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("ACTIVATE")
    class Activate {

        @Test
        @DisplayName("DRAFT → ACTIVE")
        void draftToActive() {
            assertThat(sm.apply(PromotionCampaignStatus.DRAFT, PromotionCampaignTransition.ACTIVATE))
                .isEqualTo(PromotionCampaignStatus.ACTIVE);
        }

        @Test
        @DisplayName("PAUSED → ACTIVE")
        void pausedToActive() {
            assertThat(sm.apply(PromotionCampaignStatus.PAUSED, PromotionCampaignTransition.ACTIVATE))
                .isEqualTo(PromotionCampaignStatus.ACTIVE);
        }

        @Test
        @DisplayName("INACTIVE → ACTIVE")
        void inactiveToActive() {
            assertThat(sm.apply(PromotionCampaignStatus.INACTIVE, PromotionCampaignTransition.ACTIVATE))
                .isEqualTo(PromotionCampaignStatus.ACTIVE);
        }

        @Test
        @DisplayName("ACTIVE → ACTIVE is forbidden")
        void activeToActiveIsForbidden() {
            assertThatThrownBy(() -> sm.apply(PromotionCampaignStatus.ACTIVE, PromotionCampaignTransition.ACTIVATE))
                .isInstanceOf(ProblemRestException.class);
        }

        @Test
        @DisplayName("ARCHIVED → ACTIVE is forbidden")
        void archivedToActiveIsForbidden() {
            assertThatThrownBy(() -> sm.apply(PromotionCampaignStatus.ARCHIVED, PromotionCampaignTransition.ACTIVATE))
                .isInstanceOf(ProblemRestException.class);
        }
    }

    // ── PAUSE ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PAUSE")
    class Pause {

        @Test
        @DisplayName("ACTIVE → PAUSED")
        void activeToPaused() {
            assertThat(sm.apply(PromotionCampaignStatus.ACTIVE, PromotionCampaignTransition.PAUSE))
                .isEqualTo(PromotionCampaignStatus.PAUSED);
        }

        @Test
        @DisplayName("PAUSED → PAUSED is forbidden")
        void pausedToPausedIsForbidden() {
            assertThatThrownBy(() -> sm.apply(PromotionCampaignStatus.PAUSED, PromotionCampaignTransition.PAUSE))
                .isInstanceOf(ProblemRestException.class);
        }

        @Test
        @DisplayName("DRAFT → PAUSED is forbidden")
        void draftToPausedIsForbidden() {
            assertThatThrownBy(() -> sm.apply(PromotionCampaignStatus.DRAFT, PromotionCampaignTransition.PAUSE))
                .isInstanceOf(ProblemRestException.class);
        }

        @Test
        @DisplayName("ARCHIVED → PAUSED is forbidden")
        void archivedToPausedIsForbidden() {
            assertThatThrownBy(() -> sm.apply(PromotionCampaignStatus.ARCHIVED, PromotionCampaignTransition.PAUSE))
                .isInstanceOf(ProblemRestException.class);
        }
    }

    // ── DEACTIVATE ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DEACTIVATE")
    class Deactivate {

        @Test
        @DisplayName("DRAFT → INACTIVE")
        void draftToInactive() {
            assertThat(sm.apply(PromotionCampaignStatus.DRAFT, PromotionCampaignTransition.DEACTIVATE))
                .isEqualTo(PromotionCampaignStatus.INACTIVE);
        }

        @Test
        @DisplayName("PAUSED → INACTIVE")
        void pausedToInactive() {
            assertThat(sm.apply(PromotionCampaignStatus.PAUSED, PromotionCampaignTransition.DEACTIVATE))
                .isEqualTo(PromotionCampaignStatus.INACTIVE);
        }

        @Test
        @DisplayName("ACTIVE → INACTIVE is forbidden — must pause first")
        void activeToInactiveIsForbidden() {
            assertThatThrownBy(() -> sm.apply(PromotionCampaignStatus.ACTIVE, PromotionCampaignTransition.DEACTIVATE))
                .isInstanceOf(ProblemRestException.class);
        }

        @Test
        @DisplayName("INACTIVE → INACTIVE is forbidden")
        void inactiveToInactiveIsForbidden() {
            assertThatThrownBy(() -> sm.apply(PromotionCampaignStatus.INACTIVE, PromotionCampaignTransition.DEACTIVATE))
                .isInstanceOf(ProblemRestException.class);
        }

        @Test
        @DisplayName("ARCHIVED → INACTIVE is forbidden")
        void archivedToInactiveIsForbidden() {
            assertThatThrownBy(() -> sm.apply(PromotionCampaignStatus.ARCHIVED, PromotionCampaignTransition.DEACTIVATE))
                .isInstanceOf(ProblemRestException.class);
        }
    }

    // ── ARCHIVE ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("ARCHIVE")
    class Archive {

        @Test
        @DisplayName("DRAFT → ARCHIVED")
        void draftToArchived() {
            assertThat(sm.apply(PromotionCampaignStatus.DRAFT, PromotionCampaignTransition.ARCHIVE))
                .isEqualTo(PromotionCampaignStatus.ARCHIVED);
        }

        @Test
        @DisplayName("PAUSED → ARCHIVED")
        void pausedToArchived() {
            assertThat(sm.apply(PromotionCampaignStatus.PAUSED, PromotionCampaignTransition.ARCHIVE))
                .isEqualTo(PromotionCampaignStatus.ARCHIVED);
        }

        @Test
        @DisplayName("INACTIVE → ARCHIVED")
        void inactiveToArchived() {
            assertThat(sm.apply(PromotionCampaignStatus.INACTIVE, PromotionCampaignTransition.ARCHIVE))
                .isEqualTo(PromotionCampaignStatus.ARCHIVED);
        }

        @Test
        @DisplayName("ACTIVE → ARCHIVED is forbidden — must pause first (spec constraint)")
        void activeToArchivedIsForbidden() {
            assertThatThrownBy(() -> sm.apply(PromotionCampaignStatus.ACTIVE, PromotionCampaignTransition.ARCHIVE))
                .isInstanceOf(ProblemRestException.class);
        }

        @Test
        @DisplayName("ARCHIVED → ARCHIVED is forbidden")
        void archivedToArchivedIsForbidden() {
            assertThatThrownBy(() -> sm.apply(PromotionCampaignStatus.ARCHIVED, PromotionCampaignTransition.ARCHIVE))
                .isInstanceOf(ProblemRestException.class);
        }
    }

    // ── EDGE CASES ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        @DisplayName("null status throws")
        void nullStatus() {
            assertThatThrownBy(() -> sm.apply(null, PromotionCampaignTransition.ACTIVATE))
                .isInstanceOf(ProblemRestException.class);
        }
    }
}
