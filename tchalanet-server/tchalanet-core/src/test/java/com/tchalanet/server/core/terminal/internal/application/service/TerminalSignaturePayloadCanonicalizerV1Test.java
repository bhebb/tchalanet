package com.tchalanet.server.core.terminal.internal.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalBindingId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.terminal.api.query.TerminalProofPurpose;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerminalSignaturePayloadCanonicalizerV1Test {

    private static final TerminalId TERMINAL_ID =
        TerminalId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final TerminalBindingId BINDING_ID =
        TerminalBindingId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    private static final OutletId OUTLET_ID =
        OutletId.of(UUID.fromString("00000000-0000-0000-0000-000000000003"));
    private static final SalesSessionId SESSION_ID =
        SalesSessionId.of(UUID.fromString("00000000-0000-0000-0000-000000000004"));
    private static final Instant SIGNED_AT = Instant.parse("2026-05-31T10:00:00Z");

    @Test
    void fieldOrderIsStableAndMatchesSpec() {
        var result = TerminalSignaturePayloadCanonicalizerV1.canonicalize(
            TerminalProofPurpose.SELL_TICKET,
            "POST",
            "/tenant/tickets",
            "abc123bodyhash",
            TERMINAL_ID,
            BINDING_ID,
            OUTLET_ID,
            SESSION_ID,
            "unique-nonce-1",
            SIGNED_AT
        );

        var lines = result.split("\n", -1);
        assertThat(lines).hasSize(10);
        assertThat(lines[0]).isEqualTo("SELL_TICKET");
        assertThat(lines[1]).isEqualTo("POST");
        assertThat(lines[2]).isEqualTo("/tenant/tickets");
        assertThat(lines[3]).isEqualTo("abc123bodyhash");
        assertThat(lines[4]).isEqualTo(TERMINAL_ID.value().toString());
        assertThat(lines[5]).isEqualTo(BINDING_ID.value().toString());
        assertThat(lines[6]).isEqualTo(OUTLET_ID.value().toString());
        assertThat(lines[7]).isEqualTo(SESSION_ID.value().toString());
        assertThat(lines[8]).isEqualTo("unique-nonce-1");
        assertThat(lines[9]).isEqualTo(SIGNED_AT.toString());
    }

    @Test
    void methodIsNormalisedToUpperCase() {
        var lower = TerminalSignaturePayloadCanonicalizerV1.canonicalize(
            TerminalProofPurpose.SELL_TICKET, "post", "/tenant/tickets",
            null, TERMINAL_ID, BINDING_ID, null, null, "n", SIGNED_AT);
        var upper = TerminalSignaturePayloadCanonicalizerV1.canonicalize(
            TerminalProofPurpose.SELL_TICKET, "POST", "/tenant/tickets",
            null, TERMINAL_ID, BINDING_ID, null, null, "n", SIGNED_AT);

        assertThat(lower).isEqualTo(upper);
    }

    @Test
    void nullBodyHashSubstitutedWithEmptyString() {
        var withNull = TerminalSignaturePayloadCanonicalizerV1.canonicalize(
            TerminalProofPurpose.SELL_TICKET, "POST", "/p",
            null, TERMINAL_ID, BINDING_ID, null, null, "n", SIGNED_AT);
        var withEmpty = TerminalSignaturePayloadCanonicalizerV1.canonicalize(
            TerminalProofPurpose.SELL_TICKET, "POST", "/p",
            "", TERMINAL_ID, BINDING_ID, null, null, "n", SIGNED_AT);

        // V1 null-to-empty convention: both produce the same canonical payload
        assertThat(withNull).isEqualTo(withEmpty);
        assertThat(withNull.split("\n")[3]).isEmpty();
    }

    @Test
    void nullOutletAndSessionSubstitutedWithEmptyString() {
        var result = TerminalSignaturePayloadCanonicalizerV1.canonicalize(
            TerminalProofPurpose.OFFLINE_SYNC, "POST", "/tenant/offline/sync",
            null, TERMINAL_ID, BINDING_ID, null, null, "nonce", SIGNED_AT);

        var lines = result.split("\n", -1);
        assertThat(lines[6]).isEmpty(); // outletId
        assertThat(lines[7]).isEmpty(); // sessionId
    }

    @Test
    void differentPurposesProduceDifferentPayloads() {
        var sell = TerminalSignaturePayloadCanonicalizerV1.canonicalize(
            TerminalProofPurpose.SELL_TICKET, "POST", "/p",
            null, TERMINAL_ID, BINDING_ID, null, null, "n", SIGNED_AT);
        var payout = TerminalSignaturePayloadCanonicalizerV1.canonicalize(
            TerminalProofPurpose.PAYOUT_CONFIRM, "POST", "/p",
            null, TERMINAL_ID, BINDING_ID, null, null, "n", SIGNED_AT);

        assertThat(sell).isNotEqualTo(payout);
    }

    @Test
    void differentNoncesProduceDifferentPayloads() {
        var first = TerminalSignaturePayloadCanonicalizerV1.canonicalize(
            TerminalProofPurpose.SELL_TICKET, "POST", "/p",
            null, TERMINAL_ID, BINDING_ID, null, null, "nonce-1", SIGNED_AT);
        var second = TerminalSignaturePayloadCanonicalizerV1.canonicalize(
            TerminalProofPurpose.SELL_TICKET, "POST", "/p",
            null, TERMINAL_ID, BINDING_ID, null, null, "nonce-2", SIGNED_AT);

        assertThat(first).isNotEqualTo(second);
    }
}
