package com.tchalanet.server.core.terminal.internal.application.query.handler.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.crypto.Ed25519SignatureVerifier;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalBindingId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.terminal.api.query.TerminalProofPurpose;
import com.tchalanet.server.core.terminal.api.query.VerifyTerminalDeviceProofQuery;
import com.tchalanet.server.core.terminal.api.query.VerifyTerminalDeviceProofResult;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.binding.TerminalDeviceBindingReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.nonce.TerminalDeviceNonceWriterPort;
import com.tchalanet.server.core.terminal.internal.application.service.TerminalNonceReplayGuard;
import com.tchalanet.server.core.terminal.internal.application.service.TerminalSignaturePayloadCanonicalizerV1;
import com.tchalanet.server.core.terminal.internal.domain.model.Terminal;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalKind;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalState;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalSyncState;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalBindingStatus;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalBindingType;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalDeviceBinding;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalPublicKeyAlgorithm;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class TerminalDeviceProofVerifierTest {

    private static final TenantId TENANT_ID =
        TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final TerminalId TERMINAL_ID =
        TerminalId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    private static final OutletId OUTLET_ID =
        OutletId.of(UUID.fromString("00000000-0000-0000-0000-000000000003"));
    private static final UserId USER_ID =
        UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000004"));
    private static final TerminalBindingId BINDING_ID =
        TerminalBindingId.of(UUID.fromString("00000000-0000-0000-0000-000000000005"));

    private static final Instant NOW = Instant.parse("2026-05-31T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static KeyPair KEY_PAIR;
    private static String PUBLIC_KEY_SPKI_B64;

    @BeforeAll
    static void generateKeyPair() throws Exception {
        KEY_PAIR = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        PUBLIC_KEY_SPKI_B64 = Base64.getEncoder().encodeToString(KEY_PAIR.getPublic().getEncoded());
    }

    private RecordingNonceStore nonceStore;
    private TerminalDeviceProofVerifier verifier;

    @BeforeEach
    void setUp() {
        nonceStore = new RecordingNonceStore();
        verifier = new TerminalDeviceProofVerifier(
            new StubTerminalReader(activeTerminal()),
            new StubBindingReader(activeBinding()),
            new TerminalNonceReplayGuard(nonceStore),
            new Ed25519SignatureVerifier(),
            CLOCK
        );
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void validEd25519SignatureReturnsTrusted() throws Exception {
        var query = queryWithSignature("sell-nonce", TerminalProofPurpose.SELL_TICKET);

        var result = verifier.handle(query);

        assertThat(result).isInstanceOf(VerifyTerminalDeviceProofResult.Trusted.class);
        var trusted = (VerifyTerminalDeviceProofResult.Trusted) result;
        assertThat(trusted.terminalId()).isEqualTo(TERMINAL_ID);
        assertThat(trusted.bindingId()).isEqualTo(BINDING_ID);
        assertThat(trusted.outletId()).isEqualTo(OUTLET_ID);
    }

    // ── Terminal checks ───────────────────────────────────────────────────────

    @Test
    void unknownTerminalIsRejected() {
        verifier = verifierWithTerminal(null);
        var query = queryWithoutSigning("nonce-t1", TerminalProofPurpose.SELL_TICKET, "fake-sig");

        assertRejected(query, "terminal.not_found");
    }

    @Test
    void lockedTerminalIsRejected() {
        verifier = verifierWithTerminal(lockedTerminal());
        var query = queryWithoutSigning("nonce-t2", TerminalProofPurpose.SELL_TICKET, "fake-sig");

        assertRejected(query, "terminal.locked");
    }

    // ── Binding checks ────────────────────────────────────────────────────────

    @Test
    void unknownBindingIsRejected() {
        verifier = verifierWithBinding(null);
        var query = queryWithoutSigning("nonce-b1", TerminalProofPurpose.SELL_TICKET, "fake-sig");

        assertRejected(query, "terminal.binding_not_found");
    }

    @Test
    void revokedBindingIsRejected() {
        verifier = verifierWithBinding(revokedBinding());
        var query = queryWithoutSigning("nonce-b2", TerminalProofPurpose.SELL_TICKET, "fake-sig");

        assertRejected(query, "terminal.binding_inactive");
    }

    @Test
    void bindingWithNoPublicKeyIsRejected() {
        verifier = verifierWithBinding(bindingWithoutKey());
        var query = queryWithoutSigning("nonce-b3", TerminalProofPurpose.SELL_TICKET, "fake-sig");

        assertRejected(query, "terminal.binding_no_public_key");
    }

    // ── Replay / timestamp ────────────────────────────────────────────────────

    @Test
    void reusedNonceIsRejected() throws Exception {
        var query1 = queryWithSignature("replay-nonce", TerminalProofPurpose.SELL_TICKET);
        var query2 = queryWithSignature("replay-nonce", TerminalProofPurpose.SELL_TICKET);

        verifier.handle(query1);
        assertRejected(query2, "terminal.device_replay_detected");
    }

    @Test
    void expiredSignedAtIsRejected() {
        var tooOld = NOW.minusSeconds(5 * 60 + 1);
        var query = queryRaw("old-nonce", TerminalProofPurpose.SELL_TICKET, tooOld, "fake-sig");

        assertRejected(query, "terminal.device_proof_expired_timestamp");
    }

    // ── Signature verification ────────────────────────────────────────────────

    @Test
    void invalidSignatureIsRejected() throws Exception {
        var query = queryWithSignature("sig-nonce-1", TerminalProofPurpose.SELL_TICKET);
        // Tamper with the signature
        var tampered = queryRaw("sig-nonce-2", TerminalProofPurpose.SELL_TICKET, NOW,
            Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[64]));

        assertRejected(tampered, "terminal.device_proof_invalid_signature");
    }

    @Test
    void signatureForWrongPurposeIsRejected() throws Exception {
        // Sign for SELL_TICKET but submit as PAYOUT_CONFIRM
        var signedForSell = signCanonical("purpose-nonce", TerminalProofPurpose.SELL_TICKET);
        var queryAsPayout = queryRaw("purpose-nonce", TerminalProofPurpose.PAYOUT_CONFIRM, NOW, signedForSell);

        assertRejected(queryAsPayout, "terminal.device_proof_invalid_signature");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private VerifyTerminalDeviceProofQuery queryWithSignature(String nonce, TerminalProofPurpose purpose)
        throws Exception {
        return queryRaw(nonce, purpose, NOW, signCanonical(nonce, purpose));
    }

    private String signCanonical(String nonce, TerminalProofPurpose purpose) throws Exception {
        var canonical = TerminalSignaturePayloadCanonicalizerV1.canonicalize(
            purpose, "POST", "/tenant/test", null,
            TERMINAL_ID, BINDING_ID, OUTLET_ID, null,
            nonce, NOW
        );
        var sig = Signature.getInstance("Ed25519");
        sig.initSign(KEY_PAIR.getPrivate());
        sig.update(canonical.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(sig.sign());
    }

    private VerifyTerminalDeviceProofQuery queryWithoutSigning(
        String nonce, TerminalProofPurpose purpose, String fakeSig) {
        return queryRaw(nonce, purpose, NOW, fakeSig);
    }

    private VerifyTerminalDeviceProofQuery queryRaw(
        String nonce, TerminalProofPurpose purpose, Instant signedAt, String signature) {
        return new VerifyTerminalDeviceProofQuery(
            TENANT_ID, TERMINAL_ID, BINDING_ID, purpose,
            "POST", "/tenant/test", null, OUTLET_ID, null,
            nonce, signedAt, signature
        );
    }

    private void assertRejected(VerifyTerminalDeviceProofQuery query, String expectedCode) {
        var result = verifier.handle(query);
        assertThat(result).isInstanceOf(VerifyTerminalDeviceProofResult.Rejected.class);
        assertThat(((VerifyTerminalDeviceProofResult.Rejected) result).code()).isEqualTo(expectedCode);
    }

    private TerminalDeviceProofVerifier verifierWithTerminal(Terminal terminal) {
        return new TerminalDeviceProofVerifier(
            new StubTerminalReader(terminal),
            new StubBindingReader(activeBinding()),
            new TerminalNonceReplayGuard(new RecordingNonceStore()),
            new Ed25519SignatureVerifier(), CLOCK);
    }

    private TerminalDeviceProofVerifier verifierWithBinding(TerminalDeviceBinding binding) {
        return new TerminalDeviceProofVerifier(
            new StubTerminalReader(activeTerminal()),
            new StubBindingReader(binding),
            new TerminalNonceReplayGuard(new RecordingNonceStore()),
            new Ed25519SignatureVerifier(), CLOCK);
    }

    private Terminal activeTerminal() {
        return new Terminal(TENANT_ID, TERMINAL_ID, OUTLET_ID, USER_ID,
            TerminalKind.PHYSICAL, TerminalState.ACTIVE, false, TerminalSyncState.ONLINE,
            "T-001", "Terminal", null,
            null, null, null,       // lockedAt, lockedBy, lockReason
            false, null, null, null, // salesBlocked
            false, null, null, null, // payoutBlocked
            false, null, null, null, // offlineBlocked
            NOW, Map.of("surface", "POS"), NOW, null);
    }

    private Terminal lockedTerminal() {
        return new Terminal(TENANT_ID, TERMINAL_ID, OUTLET_ID, USER_ID,
            TerminalKind.PHYSICAL, TerminalState.LOCKED, false, TerminalSyncState.ONLINE,
            "T-001", "Terminal", null,
            NOW, USER_ID, "security-lock", // lockedAt, lockedBy, lockReason — locked()=true
            false, null, null, null,
            false, null, null, null,
            false, null, null, null,
            NOW, Map.of(), NOW, null);
    }

    private TerminalDeviceBinding activeBinding() {
        return TerminalDeviceBinding.active(
            BINDING_ID, TENANT_ID, TERMINAL_ID,
            TerminalBindingType.POS_DEVICE,
            PUBLIC_KEY_SPKI_B64, TerminalPublicKeyAlgorithm.ED25519, null,
            "credential-hash", "fingerprint",
            USER_ID, NOW.minusSeconds(60), null);
    }

    private TerminalDeviceBinding revokedBinding() {
        return activeBinding().revoke(NOW.minusSeconds(30));
    }

    private TerminalDeviceBinding bindingWithoutKey() {
        return TerminalDeviceBinding.active(
            BINDING_ID, TENANT_ID, TERMINAL_ID,
            TerminalBindingType.POS_DEVICE,
            null, null, null,
            "credential-hash", "fingerprint",
            USER_ID, NOW.minusSeconds(60), null);
    }

    // ── Stubs ─────────────────────────────────────────────────────────────────

    private record StubTerminalReader(Terminal terminal) implements TerminalReaderPort {
        @Override public Optional<Terminal> findById(TenantId t, TerminalId id) {
            return Optional.ofNullable(terminal);
        }
        @Override public Terminal getById(TenantId t, TerminalId id) { return findById(t, id).orElseThrow(); }
        @Override public List<Terminal> listByOutlet(TenantId t, OutletId o, PageRequest p) { throw new UnsupportedOperationException(); }
        @Override public List<Terminal> listByTenant(TenantId t, PageRequest p) { throw new UnsupportedOperationException(); }
        @Override public TchPage<com.tchalanet.server.core.terminal.api.query.TerminalSummaryView> search(
            com.tchalanet.server.core.terminal.api.query.TerminalSearchCriteria c, TchPageRequest p) { throw new UnsupportedOperationException(); }
        @Override public List<com.tchalanet.server.core.terminal.api.query.TerminalSummaryView> listOffline() { throw new UnsupportedOperationException(); }
        @Override public List<com.tchalanet.server.core.terminal.api.query.TerminalSummaryView> listSyncPending() { throw new UnsupportedOperationException(); }
        @Override public Optional<Terminal> findCurrentForUser(UserId u) { throw new UnsupportedOperationException(); }
        @Override public int countActiveByTenant(TenantId t) { throw new UnsupportedOperationException(); }
    }

    private record StubBindingReader(TerminalDeviceBinding binding)
        implements TerminalDeviceBindingReaderPort {
        @Override public List<TerminalDeviceBinding> findActiveByTerminal(TenantId t, TerminalId id) {
            return binding != null ? List.of(binding) : List.of();
        }
        @Override public Optional<TerminalDeviceBinding> findById(TenantId t, TerminalBindingId id) {
            return Optional.ofNullable(binding);
        }
    }

    private static final class RecordingNonceStore implements TerminalDeviceNonceWriterPort {
        private final Set<String> nonces = new HashSet<>();
        @Override public boolean checkAndRecord(TenantId t, TerminalBindingId b,
            TerminalProofPurpose p, String nonce, Instant s, Instant e) {
            return nonces.add(nonce);
        }
    }
}
