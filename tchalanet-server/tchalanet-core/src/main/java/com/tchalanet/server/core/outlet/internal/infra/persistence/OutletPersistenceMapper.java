package com.tchalanet.server.core.outlet.internal.infra.persistence;

import com.tchalanet.server.common.mapper.CommonIdMapper;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.outlet.api.query.OutletSummaryView;
import com.tchalanet.server.core.outlet.internal.domain.model.BlockState;
import com.tchalanet.server.core.outlet.internal.domain.model.Outlet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {CommonIdMapper.class, com.tchalanet.server.common.mapper.CommonAuditMapper.class})
public interface OutletPersistenceMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "addressId", source = "addressId")
    @Mapping(target = "zoneId", source = "zoneId")
    @Mapping(target = "outletBlock", source = "outletBlock")
    @Mapping(target = "salesBlock", source = "salesBlock")
    @Mapping(target = "payoutBlock", source = "payoutBlock")
    @Mapping(target = "offlineSalesBlock", source = "offlineSalesBlock")
    Outlet toDomain(OutletJpaEntity entity);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "zoneId", source = "zoneId")
    @Mapping(target = "outletBlocked", source = "outletBlock.blocked")
    @Mapping(target = "outletBlockReason", source = "outletBlock.reason")
    @Mapping(target = "salesBlocked", source = "salesBlock.blocked")
    @Mapping(target = "salesBlockReason", source = "salesBlock.reason")
    @Mapping(target = "salesBlockedAt", source = "salesBlock.at")
    OutletSummaryView toSummaryView(OutletJpaEntity entity);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "addressId", source = "addressId")
    @Mapping(target = "zoneId", source = "zoneId")
    @Mapping(target = "outletBlock", source = "outletBlock")
    @Mapping(target = "salesBlock", source = "salesBlock")
    @Mapping(target = "payoutBlock", source = "payoutBlock")
    @Mapping(target = "offlineSalesBlock", source = "offlineSalesBlock")
    @Mapping(target = "metadataJson", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    OutletJpaEntity toEntity(Outlet outlet);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "addressId", source = "addressId")
    @Mapping(target = "zoneId", source = "zoneId")
    @Mapping(target = "outletBlock", source = "outletBlock")
    @Mapping(target = "salesBlock", source = "salesBlock")
    @Mapping(target = "payoutBlock", source = "payoutBlock")
    @Mapping(target = "offlineSalesBlock", source = "offlineSalesBlock")
    @Mapping(target = "metadataJson", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(Outlet outlet, @MappingTarget OutletJpaEntity entity);

    // ── BlockState ↔ BlockStateJpaEmbed ──────────────────────────────────

    default BlockState toDomain(BlockStateJpaEmbed embed) {
        if (embed == null) return BlockState.none();
        return new BlockState(
            embed.isBlocked(),
            embed.getReason(),
            embed.getAt(),
            embed.getBy() != null ? UserId.of(embed.getBy()) : null);
    }

    default BlockStateJpaEmbed toEmbed(BlockState state) {
        if (state == null) return new BlockStateJpaEmbed();
        BlockStateJpaEmbed embed = new BlockStateJpaEmbed();
        embed.setBlocked(state.blocked());
        embed.setReason(state.reason());
        embed.setAt(state.at());
        embed.setBy(state.by() != null ? state.by().value() : null);
        return embed;
    }
}
