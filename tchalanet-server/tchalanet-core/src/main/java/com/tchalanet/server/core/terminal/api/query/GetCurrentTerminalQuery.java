package com.tchalanet.server.core.terminal.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.UserId;

/** Returns the terminal currently active for the given user, if any. */
public record GetCurrentTerminalQuery(UserId userId) implements Query<TerminalView> {}
