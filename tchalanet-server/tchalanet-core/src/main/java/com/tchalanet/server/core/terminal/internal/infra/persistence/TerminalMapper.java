package com.tchalanet.server.core.terminal.internal.infra.persistence;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.terminal.api.query.TerminalSummaryView;
import com.tchalanet.server.core.terminal.internal.domain.model.Terminal;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalKind;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalState;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalSyncState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

@Mapper(componentModel = "spring")
public abstract class TerminalMapper {

    @Autowired
    protected JsonUtils jsonUtils;

    @Mapping(target = "id", source = "id", qualifiedByName = "toTerminalId")
    @Mapping(target = "tenantId", source = "tenantId", qualifiedByName = "toTenantId")
    @Mapping(target = "outletId", source = "outletId", qualifiedByName = "toOutletId")
    @Mapping(target = "assignedUserId", source = "assignedUserId", qualifiedByName = "toUserId")
    @Mapping(target = "kind", source = "kind", qualifiedByName = "toTerminalKind")
    @Mapping(target = "state", source = "state", qualifiedByName = "toTerminalState")
    @Mapping(target = "autoSessionEnabled", source = "autoSessionEnabled")
    @Mapping(target = "syncState", source = "syncState", qualifiedByName = "toTerminalSyncState")
    @Mapping(target = "metadata", source = "metadataJson", qualifiedByName = "toMetadata")
    @Mapping(target = "lockedBy", source = "lockedBy", qualifiedByName = "toUserId")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "salesBlocked", source = "salesBlocked")
    @Mapping(target = "salesBlockReason", source = "salesBlockReason")
    @Mapping(target = "salesBlockedAt", source = "salesBlockedAt")
    @Mapping(target = "salesBlockedBy", source = "salesBlockedBy", qualifiedByName = "toUserId")
    @Mapping(target = "payoutBlocked", source = "payoutBlocked")
    @Mapping(target = "payoutBlockReason", source = "payoutBlockReason")
    @Mapping(target = "payoutBlockedAt", source = "payoutBlockedAt")
    @Mapping(target = "payoutBlockedBy", source = "payoutBlockedBy", qualifiedByName = "toUserId")
    @Mapping(target = "offlineBlocked", source = "offlineBlocked")
    @Mapping(target = "offlineBlockReason", source = "offlineBlockReason")
    @Mapping(target = "offlineBlockedAt", source = "offlineBlockedAt")
    @Mapping(target = "offlineBlockedBy", source = "offlineBlockedBy", qualifiedByName = "toUserId")
    public abstract Terminal toDomain(TerminalJpaEntity entity);

    @Mapping(target = "id", source = "id", qualifiedByName = "fromTerminalId")
    @Mapping(target = "tenantId", source = "tenantId", qualifiedByName = "fromTenantId")
    @Mapping(target = "outletId", source = "outletId", qualifiedByName = "fromOutletId")
    @Mapping(target = "assignedUserId", source = "assignedUserId", qualifiedByName = "fromUserId")
    @Mapping(target = "kind", source = "kind", qualifiedByName = "fromTerminalKind")
    @Mapping(target = "state", source = "state", qualifiedByName = "fromTerminalState")
    @Mapping(target = "autoSessionEnabled", source = "autoSessionEnabled")
    @Mapping(target = "syncState", source = "syncState", qualifiedByName = "fromTerminalSyncState")
    @Mapping(target = "metadataJson", source = "metadata", qualifiedByName = "toJsonNode")
    @Mapping(target = "lockedBy", source = "lockedBy", qualifiedByName = "fromUserId")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "salesBlocked", source = "salesBlocked")
    @Mapping(target = "salesBlockReason", source = "salesBlockReason")
    @Mapping(target = "salesBlockedAt", source = "salesBlockedAt")
    @Mapping(target = "salesBlockedBy", source = "salesBlockedBy", qualifiedByName = "fromUserId")
    @Mapping(target = "payoutBlocked", source = "payoutBlocked")
    @Mapping(target = "payoutBlockReason", source = "payoutBlockReason")
    @Mapping(target = "payoutBlockedAt", source = "payoutBlockedAt")
    @Mapping(target = "payoutBlockedBy", source = "payoutBlockedBy", qualifiedByName = "fromUserId")
    @Mapping(target = "offlineBlocked", source = "offlineBlocked")
    @Mapping(target = "offlineBlockReason", source = "offlineBlockReason")
    @Mapping(target = "offlineBlockedAt", source = "offlineBlockedAt")
    @Mapping(target = "offlineBlockedBy", source = "offlineBlockedBy", qualifiedByName = "fromUserId")
    public abstract TerminalJpaEntity toEntity(Terminal domain);

    @Mapping(target = "id", source = "id", qualifiedByName = "fromTerminalId")
    @Mapping(target = "tenantId", source = "tenantId", qualifiedByName = "fromTenantId")
    @Mapping(target = "outletId", source = "outletId", qualifiedByName = "fromOutletId")
    @Mapping(target = "assignedUserId", source = "assignedUserId", qualifiedByName = "fromUserId")
    @Mapping(target = "kind", source = "kind", qualifiedByName = "fromTerminalKind")
    @Mapping(target = "state", source = "state", qualifiedByName = "fromTerminalState")
    @Mapping(target = "autoSessionEnabled", source = "autoSessionEnabled")
    @Mapping(target = "syncState", source = "syncState", qualifiedByName = "fromTerminalSyncState")
    @Mapping(target = "metadataJson", source = "metadata", qualifiedByName = "toJsonNode")
    @Mapping(target = "lockedBy", source = "lockedBy", qualifiedByName = "fromUserId")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "salesBlocked", source = "salesBlocked")
    @Mapping(target = "salesBlockReason", source = "salesBlockReason")
    @Mapping(target = "salesBlockedAt", source = "salesBlockedAt")
    @Mapping(target = "salesBlockedBy", source = "salesBlockedBy", qualifiedByName = "fromUserId")
    @Mapping(target = "payoutBlocked", source = "payoutBlocked")
    @Mapping(target = "payoutBlockReason", source = "payoutBlockReason")
    @Mapping(target = "payoutBlockedAt", source = "payoutBlockedAt")
    @Mapping(target = "payoutBlockedBy", source = "payoutBlockedBy", qualifiedByName = "fromUserId")
    @Mapping(target = "offlineBlocked", source = "offlineBlocked")
    @Mapping(target = "offlineBlockReason", source = "offlineBlockReason")
    @Mapping(target = "offlineBlockedAt", source = "offlineBlockedAt")
    @Mapping(target = "offlineBlockedBy", source = "offlineBlockedBy", qualifiedByName = "fromUserId")
    public abstract void updateEntity(Terminal domain, @MappingTarget TerminalJpaEntity entity);

    @Mapping(target = "id", source = "id", qualifiedByName = "toTerminalId")
    @Mapping(target = "outletId", source = "outletId", qualifiedByName = "toOutletId")
    @Mapping(target = "assignedUserId", source = "assignedUserId", qualifiedByName = "toUserId")
    @Mapping(target = "kind", source = "kind", qualifiedByName = "toTerminalKind")
    @Mapping(target = "state", source = "state", qualifiedByName = "toTerminalState")
    @Mapping(target = "syncState", source = "syncState", qualifiedByName = "toTerminalSyncState")
    @Mapping(target = "autoSessionEnabled", source = "autoSessionEnabled")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "locked", expression = "java(entity.getLockedAt() != null)")
    @Mapping(target = "salesBlocked", source = "salesBlocked")
    @Mapping(target = "payoutBlocked", source = "payoutBlocked")
    @Mapping(target = "offlineBlocked", source = "offlineBlocked")
    public abstract TerminalSummaryView toSummaryView(TerminalJpaEntity entity);

    @Named("toTerminalId")
    protected TerminalId toTerminalId(UUID value) {
        return TerminalId.nullableOf(value);
    }

    @Named("fromTerminalId")
    protected UUID fromTerminalId(TerminalId id) {
        return id == null ? null : id.value();
    }

    @Named("toTenantId")
    protected TenantId toTenantId(UUID value) {
        return TenantId.nullableOf(value);
    }

    @Named("fromTenantId")
    protected UUID fromTenantId(TenantId id) {
        return id == null ? null : id.value();
    }

    @Named("toOutletId")
    protected OutletId toOutletId(UUID value) {
        return OutletId.nullableOf(value);
    }

    @Named("fromOutletId")
    protected UUID fromOutletId(OutletId id) {
        return id == null ? null : id.value();
    }

    @Named("toUserId")
    protected UserId toUserId(UUID value) {
        return UserId.nullableOf(value);
    }

    @Named("fromUserId")
    protected UUID fromUserId(UserId id) {
        return id == null ? null : id.value();
    }

    @Named("toTerminalKind")
    protected TerminalKind toTerminalKind(String value) {
        return value == null || value.isBlank() ? TerminalKind.PHYSICAL : TerminalKind.valueOf(value);
    }

    @Named("fromTerminalKind")
    protected String fromTerminalKind(TerminalKind value) {
        return value == null ? TerminalKind.PHYSICAL.name() : value.name();
    }

    @Named("toTerminalState")
    protected TerminalState toTerminalState(String value) {
        return value == null || value.isBlank() ? TerminalState.REGISTERED : TerminalState.valueOf(value);
    }

    @Named("fromTerminalState")
    protected String fromTerminalState(TerminalState value) {
        return value == null ? TerminalState.REGISTERED.name() : value.name();
    }

    @Named("toTerminalSyncState")
    protected TerminalSyncState toTerminalSyncState(String value) {
        return value == null || value.isBlank()
            ? TerminalSyncState.ONLINE
            : TerminalSyncState.valueOf(value);
    }

    @Named("fromTerminalSyncState")
    protected String fromTerminalSyncState(TerminalSyncState value) {
        return value == null ? TerminalSyncState.ONLINE.name() : value.name();
    }

    @Named("toMetadata")
    protected Map<String, Object> toMetadata(JsonNode node) {
        if (node == null || node.isNull()) {
            return Map.of();
        }

        Map<?, ?> raw = jsonUtils.treeToValue(node, Map.class);
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> result = new HashMap<>();
        raw.forEach((key, value) -> result.put(String.valueOf(key), value));

        return Map.copyOf(result);
    }

    @Named("toJsonNode")
    protected JsonNode toJsonNode(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return jsonUtils.emptyObject();
        }
        return jsonUtils.toJsonNode(metadata);
    }
}
