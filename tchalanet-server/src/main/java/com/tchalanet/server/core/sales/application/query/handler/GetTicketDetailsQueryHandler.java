package com.tchalanet.server.core.sales.application.query.handler;

import com.tchalanet.server.common.app.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.application.query.model.GetTicketDetailsByIdQuery;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Handler to fetch ticket details by id or public code. */
@UseCase
@RequiredArgsConstructor
@Component
public class GetTicketDetailsQueryHandler {

  // Ideally we implement QueryHandler<GetTicketDetailsByIdQuery, GetTicketDetailsByIdQuery.TicketDetailsDto>
  // but controller usage in the project uses custom methods: findById / findByPublicCode.

  public Optional<GetTicketDetailsByIdQuery.TicketDetailsDto> findById(UUID id) {
    // TODO: implement retrieval and mapping
    throw new UnsupportedOperationException("GetTicketDetailsQueryHandler.findById not implemented yet");
  }

  public Optional<GetTicketDetailsByIdQuery.TicketDetailsDto> findByPublicCode(String publicCode) {
    // TODO: implement retrieval and mapping
    throw new UnsupportedOperationException("GetTicketDetailsQueryHandler.findByPublicCode not implemented yet");
  }
}

