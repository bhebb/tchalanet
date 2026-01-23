package com.tchalanet.server.catalog.address.api;

import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;

import java.util.List;
import java.util.Optional;

public interface AddressCatalog {
    List<AddressView> listActive();

    Optional<AddressView> findById(AddressId id);

    // Paginated search by criteria
    TchPage<AddressView> search(AddressSearchCriteria criteria, TchPageRequest pageReq);
}
