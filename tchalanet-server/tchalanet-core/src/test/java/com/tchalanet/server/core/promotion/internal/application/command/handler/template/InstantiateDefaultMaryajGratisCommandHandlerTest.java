package com.tchalanet.server.core.promotion.internal.application.command.handler.template;

import com.tchalanet.server.common.time.TimeProvider;
import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.promotion.api.command.lifecycle.CreatePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.api.command.lifecycle.UpdatePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.api.command.rule.AddPromotionRuleCommand;
import com.tchalanet.server.core.promotion.api.command.rule.DeletePromotionRuleCommand;
import com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleCommand;
import com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleEffectsCommand;
import com.tchalanet.server.core.promotion.api.command.rule.UpdatePromotionRuleEligibilityCommand;
import com.tchalanet.server.core.promotion.api.command.template.InstantiateDefaultMaryajGratisCommand;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignStatus;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignView;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectConfigView;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionQuantityMode;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionQuantityTier;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionRuleView;
import com.tchalanet.server.core.promotion.internal.application.port.out.PromotionCacheEvictorPort;
import com.tchalanet.server.core.promotion.internal.application.port.out.lifecycle.PromotionCampaignReadPort;
import com.tchalanet.server.core.promotion.internal.application.port.out.lifecycle.PromotionCampaignWritePort;
import com.tchalanet.server.core.promotion.internal.application.service.lifecycle.PromotionCampaignActivationPolicy;
import com.tchalanet.server.core.promotion.internal.application.service.template.MaryajGratisDefaultTemplate;
import com.tchalanet.server.core.promotion.internal.domain.service.PromotionCampaignStateMachine;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InstantiateDefaultMaryajGratisCommandHandler")
class InstantiateDefaultMaryajGratisCommandHandlerTest {

    private static final TenantId TENANT = TenantId.of(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-06-10T00:00:00Z");

    private static class WritePortStub implements PromotionCampaignWritePort {
        CreatePromotionCampaignCommand createdWith;
        PromotionCampaignStatus changedTo;
        PromotionCampaignView draft;
        PromotionCampaignView active;

        @Override
        public PromotionCampaignView create(CreatePromotionCampaignCommand cmd) {
            this.createdWith = cmd;
            return draft;
        }

        @Override
        public PromotionCampaignView update(UpdatePromotionCampaignCommand cmd) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PromotionCampaignView changeStatus(
            TenantId tenantId, PromotionCampaignId campaignId, PromotionCampaignStatus status) {
            this.changedTo = status;
            return active;
        }

        @Override
        public PromotionCampaignView addRule(AddPromotionRuleCommand cmd) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PromotionCampaignView updateRule(UpdatePromotionRuleCommand cmd) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PromotionCampaignView deleteRule(DeletePromotionRuleCommand cmd) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PromotionCampaignView updateRuleEffects(UpdatePromotionRuleEffectsCommand cmd) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PromotionCampaignView updateRuleEligibility(UpdatePromotionRuleEligibilityCommand cmd) {
            throw new UnsupportedOperationException();
        }
    }

    private static PromotionCampaignReadPort readPort(PromotionCampaignView existing) {
        return new PromotionCampaignReadPort() {
            @Override
            public TchPage<PromotionCampaignView> findCampaigns(Pageable pageable) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<PromotionCampaignView> findById(PromotionCampaignId id) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<PromotionCampaignView> findByCode(String code) {
                return MaryajGratisDefaultTemplate.CODE.equals(code)
                    ? Optional.ofNullable(existing)
                    : Optional.empty();
            }

            @Override
            public PromotionCampaignView getRequired(PromotionCampaignId promotionCampaignId) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private InstantiateDefaultMaryajGratisCommandHandler handler(
        PromotionCampaignView existing, WritePortStub writePort) {
        return new InstantiateDefaultMaryajGratisCommandHandler(
            readPort(existing),
            writePort,
            new PromotionCampaignStateMachine(),
            new PromotionCampaignActivationPolicy(),
            new PromotionCacheEvictorPort() {
                @Override
                public void evictAfterCampaignMutation(TenantId tenantId, PromotionCampaignId campaignId) { }

                @Override
                public void evictRuntimeForTenant(TenantId tenantId) { }

                @Override
                public void evictCampaignDetail(TenantId tenantId, PromotionCampaignId campaignId) { }

                @Override
                public void clearAdminLists() { }
            },
            new TimeProvider(Clock.fixed(NOW, ZoneOffset.UTC)));
    }

    @Test
    @DisplayName("creates the campaign from the template and activates it")
    void createsAndActivates() {
        var writePort = new WritePortStub();
        writePort.draft = view(PromotionCampaignStatus.DRAFT);
        writePort.active = view(PromotionCampaignStatus.ACTIVE);

        var out = handler(null, writePort)
            .handle(new InstantiateDefaultMaryajGratisCommand(TENANT));

        assertThat(out.status()).isEqualTo(PromotionCampaignStatus.ACTIVE);
        assertThat(writePort.changedTo).isEqualTo(PromotionCampaignStatus.ACTIVE);

        var create = writePort.createdWith;
        assertThat(create.tenantId()).isEqualTo(TENANT);
        assertThat(create.name()).isEqualTo(MaryajGratisDefaultTemplate.CODE);
        assertThat(create.startsAt()).isEqualTo(NOW);
        assertThat(create.rules()).hasSize(1);
        var effect = create.rules().get(0).effectItems().get(0);
        assertThat(effect.type()).isEqualTo(PromotionEffectType.FREE_GAME_LINE);
        assertThat(effect.params())
            .containsEntry("gameCode", "HT_MARYAJ_GRATUIT")
            .containsEntry("payoutBaseAmount", "50")
            .containsEntry("quantityMode", "TIERED_PAID_AMOUNT")
            .containsEntry("quantity", "1")
            .containsEntry("maxQuantity", "3")
            .containsEntry("choiceMode", "AUTO_GENERATE")
            .containsEntry("generationStrategy", "RANDOM")
            .containsEntry("regenerableBeforeConfirm", "true")
            .containsEntry("maxRegenerationsBeforeConfirm", "3");
        assertThat(effect.params().get("quantityTiers")).isInstanceOf(List.class);
    }

    @Test
    @DisplayName("is idempotent: returns the existing campaign without creating")
    void idempotentWhenCodeExists() {
        var existing = view(PromotionCampaignStatus.ACTIVE);
        var writePort = new WritePortStub();

        var out = handler(existing, writePort)
            .handle(new InstantiateDefaultMaryajGratisCommand(TENANT));

        assertThat(out).isSameAs(existing);
        assertThat(writePort.createdWith).isNull();
        assertThat(writePort.changedTo).isNull();
    }

    @Test
    @DisplayName("creates a per paid amount Maryaj gratis campaign")
    void createsPerPaidAmountCampaign() {
        var writePort = new WritePortStub();
        writePort.draft = view(PromotionCampaignStatus.DRAFT);
        writePort.active = view(PromotionCampaignStatus.ACTIVE);

        handler(null, writePort)
            .handle(new InstantiateDefaultMaryajGratisCommand(
                TENANT,
                null,
                PromotionQuantityMode.PER_PAID_AMOUNT,
                null,
                new java.math.BigDecimal("1000"),
                2,
                10,
                null,
                null,
                null,
                null,
                null
            ));

        var effect = writePort.createdWith.rules().get(0).effectItems().get(0);
        assertThat(effect.params())
            .containsEntry("quantityMode", "PER_PAID_AMOUNT")
            .containsEntry("stepPaidAmount", "1000")
            .containsEntry("quantityPerStep", "2")
            .containsEntry("maxQuantity", "10");
    }

    @Test
    @DisplayName("creates a tiered paid amount Maryaj gratis campaign")
    void createsTieredPaidAmountCampaign() {
        var writePort = new WritePortStub();
        writePort.draft = view(PromotionCampaignStatus.DRAFT);
        writePort.active = view(PromotionCampaignStatus.ACTIVE);

        handler(null, writePort)
            .handle(new InstantiateDefaultMaryajGratisCommand(
                TENANT,
                null,
                PromotionQuantityMode.TIERED_PAID_AMOUNT,
                null,
                null,
                null,
                null,
                List.of(
                    new PromotionQuantityTier(new java.math.BigDecimal("100"), new java.math.BigDecimal("199"), 1),
                    new PromotionQuantityTier(new java.math.BigDecimal("200"), new java.math.BigDecimal("499"), 2),
                    new PromotionQuantityTier(new java.math.BigDecimal("500"), null, 3)
                ),
                null,
                null,
                null,
                null
            ));

        var effect = writePort.createdWith.rules().get(0).effectItems().get(0);
        assertThat(effect.params())
            .containsEntry("quantityMode", "TIERED_PAID_AMOUNT")
            .containsEntry("maxQuantity", "3");
        assertThat(effect.params().get("quantityTiers")).isInstanceOf(List.class);
    }

    private static PromotionCampaignView view(PromotionCampaignStatus status) {
        return new PromotionCampaignView(
            PromotionCampaignId.of(UUID.randomUUID()),
            MaryajGratisDefaultTemplate.CODE,
            MaryajGratisDefaultTemplate.CODE,
            status,
            100,
            NOW,
            NOW.plusSeconds(3600),
            List.of(new PromotionRuleView(
                null,
                "maryaj-gratis-default",
                100,
                List.of(),
                List.of(new PromotionEffectConfigView(
                    PromotionEffectType.FREE_GAME_LINE,
                    Map.of("gameCode", "HT_MARYAJ_GRATUIT", "generationStrategy", "RANDOM")))))
        );
    }
}
