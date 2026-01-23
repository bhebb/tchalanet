
package com.tchalanet.server.catalog.address.internal.mapper;

import com.tchalanet.server.catalog.address.api.AddressView;
import com.tchalanet.server.catalog.address.internal.persistence.AddressJpaEntity;
import com.tchalanet.server.common.mapper.CommonIdMapper;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public interface AddressMapper {

    AddressView toView(AddressJpaEntity e);

    List<AddressView> toViews(List<AddressJpaEntity> matches);
}
