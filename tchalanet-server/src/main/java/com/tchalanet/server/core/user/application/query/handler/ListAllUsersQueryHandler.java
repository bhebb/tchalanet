package com.tchalanet.server.core.user.application.query.handler;

import com.tchalanet.server.common.app.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.query.model.PagedListAllUsersQuery;
import com.tchalanet.server.core.user.domain.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@UseCase
@RequiredArgsConstructor
public class ListAllUsersQueryHandler implements QueryHandler<PagedListAllUsersQuery, Page<AppUser>> {

    private final UserReaderPort userReaderPort;

    @Override
    public Page<AppUser> handle(PagedListAllUsersQuery query) {
        var pageable = PageRequest.of(query.page(), query.size());
        return userReaderPort.findAll(pageable);
    }
}
