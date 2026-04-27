package com.tchalanet.server.core.user.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.user.application.command.model.UpdateUserPreferencesCommand;
import com.tchalanet.server.core.user.application.port.out.UserPreferenceReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserPreferenceWriterPort;
import com.tchalanet.server.core.user.domain.model.UserPreference;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateUserPreferencesCommandHandler implements VoidCommandHandler<UpdateUserPreferencesCommand> {

    private final UserPreferenceReaderPort reader;
    private final UserPreferenceWriterPort writer;

    @Override
    @TchTx
    public void handle(UpdateUserPreferencesCommand cmd) {
        UserId userId = cmd.userId();
        UserPreference pref = reader.findByUserId(userId).orElseGet(() -> UserPreference.forUser(userId));

        pref.applyOverrides(
            cmd.prefThemeMode().orElse(null),
            cmd.prefDensity().orElse(null),
            cmd.prefLocale().orElse(null),
            cmd.prefTimeZone().orElse(null),
            cmd.prefCurrency().orElse(null)
        );

        writer.upsert(pref);
    }
}
