package com.tchalanet.server.core.haiti.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TchalaEntryId;
import com.tchalanet.server.core.haiti.domain.tchala.model.TchalaEntry;
import java.util.Optional;

/** Query to get a Tchala entry by UUID. */
public record GetTchalaEntryQuery(TchalaEntryId entryId) implements Query<Optional<TchalaEntry>> {}
