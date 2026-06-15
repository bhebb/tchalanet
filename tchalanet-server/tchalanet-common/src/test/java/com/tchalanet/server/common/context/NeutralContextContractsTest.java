package com.tchalanet.server.common.context;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NeutralContextContractsTest {

    private static final UserId USER_ID = UserId.of(UUID.randomUUID());
    private static final SellerTerminalId TERMINAL_ID = SellerTerminalId.of(UUID.randomUUID());
    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());

    @Nested
    class BootstrappedActorContracts {

        @Test
        void appUserFactory_setsCorrectFields() {
            var actor = BootstrappedActor.appUser(USER_ID, "firebase", "https://issuer", "sub-123");

            assertThat(actor.actorType()).isEqualTo(TchActorType.APP_USER);
            assertThat(actor.appUserId()).isEqualTo(USER_ID);
            assertThat(actor.sellerTerminalId()).isNull();
            assertThat(actor.tenantId()).isNull();
            assertThat(actor.provider()).isEqualTo("firebase");
            assertThat(actor.externalSubject()).isEqualTo("sub-123");
            assertThat(actor.isAppUser()).isTrue();
            assertThat(actor.isSellerTerminal()).isFalse();
        }

        @Test
        void sellerTerminalFactory_setsCorrectFields() {
            var actor = BootstrappedActor.sellerTerminal(TERMINAL_ID, TENANT_ID, "firebase", "https://issuer", "sub-456");

            assertThat(actor.actorType()).isEqualTo(TchActorType.SELLER_TERMINAL);
            assertThat(actor.sellerTerminalId()).isEqualTo(TERMINAL_ID);
            assertThat(actor.tenantId()).isEqualTo(TENANT_ID);
            assertThat(actor.appUserId()).isNull();
            assertThat(actor.isSellerTerminal()).isTrue();
            assertThat(actor.isAppUser()).isFalse();
        }
    }

    @Nested
    class ResolvedAccessContextContracts {

        @Test
        void appUserContext_setsCorrectFields() {
            var ctx = new ResolvedAccessContext(
                TchActorType.APP_USER, USER_ID, null, TENANT_ID,
                false, false,
                Set.of("TENANT_ADMIN"), Set.of("tenant.manage")
            );

            assertThat(ctx.actorType()).isEqualTo(TchActorType.APP_USER);
            assertThat(ctx.appUserId()).isEqualTo(USER_ID);
            assertThat(ctx.sellerTerminalId()).isNull();
            assertThat(ctx.isAppUser()).isTrue();
            assertThat(ctx.isSellerTerminal()).isFalse();
            assertThat(ctx.isSystem()).isFalse();
            assertThat(ctx.roleCodes()).containsExactly("TENANT_ADMIN");
            assertThat(ctx.permissionKeys()).containsExactly("tenant.manage");
        }

        @Test
        void sellerTerminalContext_setsCorrectFields() {
            var ctx = new ResolvedAccessContext(
                TchActorType.SELLER_TERMINAL, null, TERMINAL_ID, TENANT_ID,
                false, false,
                Set.of(), Set.of("terminal.sell")
            );

            assertThat(ctx.actorType()).isEqualTo(TchActorType.SELLER_TERMINAL);
            assertThat(ctx.sellerTerminalId()).isEqualTo(TERMINAL_ID);
            assertThat(ctx.appUserId()).isNull();
            assertThat(ctx.effectiveTenantId()).isEqualTo(TENANT_ID);
            assertThat(ctx.isSellerTerminal()).isTrue();
            assertThat(ctx.roleCodes()).isEmpty();
            assertThat(ctx.permissionKeys()).containsExactly("terminal.sell");
        }

        @Test
        void nullSetsComeOutAsEmptyImmutableSets() {
            var ctx = new ResolvedAccessContext(
                TchActorType.APP_USER, USER_ID, null, null,
                false, false, null, null
            );

            assertThat(ctx.roleCodes()).isEmpty();
            assertThat(ctx.permissionKeys()).isEmpty();
        }

        @Test
        void setsAreDefensivelyImmutable() {
            var mutableRoles = new java.util.HashSet<>(Set.of("TENANT_ADMIN"));
            var ctx = new ResolvedAccessContext(
                TchActorType.APP_USER, USER_ID, null, null,
                false, false, mutableRoles, Set.of()
            );
            mutableRoles.add("SUPER_ADMIN");

            assertThat(ctx.roleCodes()).containsExactly("TENANT_ADMIN");
        }

        @Test
        void superAdminContext_flagSetCorrectly() {
            var ctx = new ResolvedAccessContext(
                TchActorType.APP_USER, USER_ID, null, TENANT_ID,
                true, true, Set.of("SUPER_ADMIN"), Set.of()
            );

            assertThat(ctx.superAdmin()).isTrue();
            assertThat(ctx.tenantOverride()).isTrue();
        }
    }

    @Nested
    class TchContextRequestAttributesContracts {

        @Test
        void constantValues() {
            assertThat(TchContextRequestAttributes.BOOTSTRAPPED_ACTOR).isEqualTo("tch.bootstrappedActor");
            assertThat(TchContextRequestAttributes.RESOLVED_ACCESS).isEqualTo("tch.resolvedAccess");
            assertThat(TchContextRequestAttributes.TENANT_OVERRIDE).isEqualTo("tch.tenantOverride");
        }
    }

    @Nested
    class TchRequestContextNewFields {

        @Test
        void newFieldsDefaultToNullAndEmptySetsWhenNotSet() {
            var ctx = contextBase().build();

            assertThat(ctx.actorType()).isNull();
            assertThat(ctx.sellerTerminalId()).isNull();
            assertThat(ctx.roleCodes()).isEmpty();
            assertThat(ctx.permissionKeys()).isEmpty();
            assertThat(ctx.externalSubject()).isNull();
        }

        @Test
        void withResolvedAccess_setsActorFields() {
            var base = contextBase().build();
            var updated = base.withResolvedAccess(
                TchActorType.APP_USER, null,
                Set.of("TENANT_ADMIN"), Set.of("tenant.manage")
            );

            assertThat(updated.actorType()).isEqualTo(TchActorType.APP_USER);
            assertThat(updated.roleCodes()).containsExactly("TENANT_ADMIN");
            assertThat(updated.permissionKeys()).containsExactly("tenant.manage");
            // unchanged fields are preserved
            assertThat(updated.requestId()).isEqualTo(base.requestId());
        }

        @Test
        void withResolvedAccess_doesNotMutateOriginal() {
            var base = contextBase().build();
            base.withResolvedAccess(TchActorType.APP_USER, null, Set.of("X"), Set.of("y"));

            assertThat(base.actorType()).isNull();
            assertThat(base.roleCodes()).isEmpty();
        }

        private ContextBuilder contextBase() {
            return new ContextBuilder();
        }

        private static class ContextBuilder {
            TchRequestContext build() {
                return new TchRequestContext(
                    "demo", UUID.randomUUID(), "demo", UUID.randomUUID(),
                    null, UUID.randomUUID(), java.util.Set.of(), java.util.Set.of(),
                    java.util.Locale.ENGLISH, "req-1", "127.0.0.1", "test",
                    false, null, "active",
                    com.tchalanet.server.common.context.scope.ApiScope.TENANT, null,
                    TENANT_ID, java.time.ZoneId.of("UTC"),
                    java.util.Currency.getInstance("USD"), null,
                    null, null, null, null, null
                );
            }
        }
    }
}
