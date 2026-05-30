package com.tchalanet.server.core.terminal.internal.application.command.handler.challenge;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.TerminalActivationChallengeId;
import com.tchalanet.server.core.terminal.api.command.CreateTerminalActivationChallengeCommand;
import com.tchalanet.server.core.terminal.api.command.CreateTerminalActivationChallengeResult;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.challenge.TerminalActivationChallengeWriterPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.challenge.TerminalChallengeCodeGeneratorPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.challenge.TerminalChallengeDeliveryPort;
import com.tchalanet.server.core.terminal.internal.application.service.challenge.TerminalChallengeCodeHasher;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalActivationChallenge;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeDeliveryPolicy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@UseCase
public class CreateTerminalActivationChallengeCommandHandler
    implements CommandHandler<CreateTerminalActivationChallengeCommand, CreateTerminalActivationChallengeResult> {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);
    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    private final TerminalReaderPort terminalReader;
    private final TerminalActivationChallengeWriterPort challengeWriter;
    private final TerminalChallengeCodeGeneratorPort codeGenerator;
    private final TerminalChallengeDeliveryPort deliveryPort;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public CreateTerminalActivationChallengeCommandHandler(
        TerminalReaderPort terminalReader,
        TerminalActivationChallengeWriterPort challengeWriter,
        TerminalChallengeCodeGeneratorPort codeGenerator,
        TerminalChallengeDeliveryPort deliveryPort,
        IdGenerator idGenerator,
        Clock clock
    ) {
        this.terminalReader = Objects.requireNonNull(terminalReader, "terminalReader is required");
        this.challengeWriter = Objects.requireNonNull(challengeWriter, "challengeWriter is required");
        this.codeGenerator = Objects.requireNonNull(codeGenerator, "codeGenerator is required");
        this.deliveryPort = Objects.requireNonNull(deliveryPort, "deliveryPort is required");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    @Override
    @TchTx
    public CreateTerminalActivationChallengeResult handle(CreateTerminalActivationChallengeCommand command) {
        var terminal = terminalReader.getRequired(command.tenantId(), command.terminalId());
        if (!terminal.assignedTo(command.userId())) {
            throw new IllegalStateException("Terminal is assigned to another user");
        }

        // Cancel any existing PENDING challenge for this terminal/user/type combination
        // to avoid violating ux_terminal_challenge__pending_terminal_user_type.
        challengeWriter.revokeAllPending(
            command.tenantId(), command.terminalId(), command.userId(), command.challengeType());

        var now = Instant.now(clock);
        var challengeId = TerminalActivationChallengeId.of(idGenerator.newUuid());
        var channel = TerminalChallengeDeliveryPolicy.defaultChannel(command.challengeType(), command.deliveryMode());
        var clearCode = codeGenerator.generate(command.challengeType());
        var codeHash = TerminalChallengeCodeHasher.hash(
            command.tenantId(),
            command.terminalId(),
            command.userId(),
            challengeId,
            clearCode
        );
        var challenge = TerminalActivationChallenge.pending(
            challengeId,
            command.tenantId(),
            command.terminalId(),
            command.userId(),
            command.challengeType(),
            channel,
            codeHash,
            now,
            now.plus(DEFAULT_TTL),
            DEFAULT_MAX_ATTEMPTS
        );

        var saved = challengeWriter.save(challenge);
        var delivery = deliveryPort.deliver(saved, clearCode);

        return new CreateTerminalActivationChallengeResult(
            saved.id(),
            saved.challengeType(),
            saved.channel(),
            saved.expiresAt(),
            delivery.deliveryRef()
        );
    }
}
