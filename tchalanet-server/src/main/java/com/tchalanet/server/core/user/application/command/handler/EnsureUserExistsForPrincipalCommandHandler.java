package com.tchalanet.server.core.user.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.time.TimeProvider;
import com.tchalanet.server.core.user.application.command.model.EnsureUserExistsForPrincipalCommand;
import com.tchalanet.server.core.user.application.command.model.EnsureUserExistsForPrincipalResult;
import com.tchalanet.server.core.user.application.port.out.UserPreferenceReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserPreferenceWriterPort;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserWriterPort;
import com.tchalanet.server.core.user.domain.model.AppUser;
import com.tchalanet.server.core.user.domain.model.UserPreference;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class EnsureUserExistsForPrincipalCommandHandler
    implements CommandHandler<
    EnsureUserExistsForPrincipalCommand, EnsureUserExistsForPrincipalResult> {

    private final UserReaderPort userReaderPort;
    private final UserWriterPort userWriterPort;
    private final UserPreferenceReaderPort userPreferenceReaderPort;
    private final UserPreferenceWriterPort userPreferenceWriterPort;
    private final TimeProvider timeProvider;

    @Override
    public EnsureUserExistsForPrincipalResult handle(EnsureUserExistsForPrincipalCommand command) {
        var existingOpt = userReaderPort.findByKeycloakSub(command.keycloakSub());

        if (existingOpt.isPresent()) {
            var user = existingOpt.get();

            var updated =
                user.syncProfile(
                        command.username(),
                        command.email(),
                        command.phone(),
                        command.firstName(),
                        command.lastName(),
                        command.displayName(),
                        null) // avatarUrl non fourni dans la command V1
                    .touchLogin(timeProvider.nowInstant());

            var saved = userWriterPort.save(updated);

            // Ensure preferences exist
            var prefOpt = userPreferenceReaderPort.findByUserId(saved.getId());
            if (prefOpt.isEmpty()) {
                var pref = UserPreference.forUser(saved.getId())
                    .applyOverrides(null, null, command.locale(), command.timeZone(), null);
                userPreferenceWriterPort.upsert(pref);
            }

            return new EnsureUserExistsForPrincipalResult(false, saved.getId());
        }

        var now = timeProvider.nowInstant();

        var newUser =
            AppUser.createNew(
                null,
                command.keycloakSub(),
                command.username(),
                command.email(),
                command.phone(),
                command.firstName(),
                command.lastName(),
                command.displayName(),
                null, // avatarUrl
                now);

        var saved = userWriterPort.save(newUser);

        // Create default preferences
        var defaultPref = UserPreference.forUser(saved.getId())
            .applyOverrides(null, null, command.locale(), command.timeZone(), null);
        userPreferenceWriterPort.upsert(defaultPref);

        return new EnsureUserExistsForPrincipalResult(true, saved.getId());
    }
}
