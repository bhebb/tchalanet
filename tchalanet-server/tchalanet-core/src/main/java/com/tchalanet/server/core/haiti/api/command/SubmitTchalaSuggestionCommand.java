package com.tchalanet.server.core.haiti.api.command;

import com.tchalanet.server.common.bus.Command;

public record SubmitTchalaSuggestionCommand(String lang, String dream, String numbers, String note)
    implements Command<SubmitTchalaSuggestionResult> {}
