package com.tchalanet.server.core.terminal.internal.application.command.handler.challenge;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.TerminalBindingId;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.core.terminal.api.command.VerifyTerminalActivationChallengeCommand;
import com.tchalanet.server.core.terminal.api.command.VerifyTerminalActivationChallengeResult;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalWriterPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.binding.TerminalDeviceBindingReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.binding.TerminalDeviceBindingWriterPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.challenge.TerminalActivationChallengeReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.challenge.TerminalActivationChallengeWriterPort;
import com.tchalanet.server.core.terminal.internal.application.service.challenge.TerminalChallengeCodeHasher;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalDeviceBinding;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@UseCase
public class VerifyTerminalActivationChallengeCommandHandler
    implements CommandHandler<VerifyTerminalActivationChallengeCommand, VerifyTerminalActivationChallengeResult> {

    private final TerminalActivationChallengeReaderPort challengeReader;
    private final TerminalActivationChallengeWriterPort challengeWriter;
    private final TerminalReaderPort terminalReader;
    private final TerminalWriterPort terminalWriter;
    private final TerminalDeviceBindingReaderPort bindingReader;
    private final TerminalDeviceBindingWriterPort bindingWriter;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public VerifyTerminalActivationChallengeCommandHandler(
        TerminalActivationChallengeReaderPort challengeReader,
        TerminalActivationChallengeWriterPort challengeWriter,
        TerminalReaderPort terminalReader,
        TerminalWriterPort terminalWriter,
        TerminalDeviceBindingReaderPort bindingReader,
        TerminalDeviceBindingWriterPort bindingWriter,
        IdGenerator idGenerator,
        Clock clock
    ) {
        this.challengeReader = Objects.requireNonNull(challengeReader, "challengeReader is required");
        this.challengeWriter = Objects.requireNonNull(challengeWriter, "challengeWriter is required");
        this.terminalReader = Objects.requireNonNull(terminalReader, "terminalReader is required");
        this.terminalWriter = Objects.requireNonNull(terminalWriter, "terminalWriter is required");
        this.bindingReader = Objects.requireNonNull(bindingReader, "bindingReader is required");
        this.bindingWriter = Objects.requireNonNull(bindingWriter, "bindingWriter is required");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    @Override
    @TchTx
    public VerifyTerminalActivationChallengeResult handle(VerifyTerminalActivationChallengeCommand command) {
        var challenge = challengeReader.getRequired(command.tenantId(), command.challengeId());
        if (!challenge.userId().equals(command.userId())) {
            throw ProblemRestException.badRequest("terminal.challenge.user_mismatch");
        }

        var now = Instant.now(clock);
        var candidateHash = TerminalChallengeCodeHasher.hash(
            command.tenantId(),
            challenge.terminalId(),
            command.userId(),
            command.challengeId(),
            command.clearCode()
        );
        var verification = challenge.verifyHash(candidateHash, now);
        var updatedChallenge = challengeWriter.save(verification.challenge());
        if (!verification.verified()) {
            throw ProblemRestException.badRequest("terminal.challenge.invalid_or_expired");
        }

        var terminal = terminalReader.getRequired(command.tenantId(), updatedChallenge.terminalId());
        var binding = TerminalDeviceBinding.active(
            TerminalBindingId.of(idGenerator.newUuid()),
            command.tenantId(),
            terminal.id(),
            command.bindingType(),
            command.bindingPublicKey(),
            command.bindingSecretHash(),
            command.deviceFingerprintHash(),
            now,
            null
        );
        if (!binding.compatibleWith(terminal.kind(), terminal.effectiveSurface())) {
            throw ProblemRestException.badRequest("terminal.binding.incompatible");
        }

        bindingReader.findActiveByTerminal(command.tenantId(), terminal.id()).stream()
            .map(existing -> existing.revoke(now))
            .forEach(bindingWriter::save);

        binding = bindingWriter.save(binding);
        terminalWriter.save(terminal.register(now));

        return new VerifyTerminalActivationChallengeResult(
            terminal.id(),
            binding.id(),
            binding.bindingType()
        );
    }
}
