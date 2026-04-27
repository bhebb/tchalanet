package com.tchalanet.server.common.mapper;

import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.common.time.DaysOfWeekFormatter;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Central helper for conversions between UUID/String and domain wrappers or specialized types.
 * MapStruct can use this via `uses = CommonIdMapper.class` on @Mapper.
 */
@Component
public class CommonIdMapper {

    // TenantId
    public UUID mapFromTenantId(TenantId id) {
        return id == null ? null : id.value();
    }

    public TenantId mapToTenantId(UUID id) {
        return TenantId.nullableOf(id);
    }


    //address
    public UUID mapFromAddressId(AddressId id) {
        return id == null ? null : id.value();
    }

    public AddressId mapToAddressId(UUID id) {
        return AddressId.nullableOf(id);
    }

    // UserId
    public UUID mapFromUserId(UserId id) {
        return id == null ? null : id.value();
    }

    public UserId mapToUserId(UUID id) {
        return UserId.nullableOf(id);
    }

    // DrawId
    public UUID mapFromDrawId(DrawId id) {
        return id == null ? null : id.value();
    }

    public DrawId mapToDrawId(UUID id) {
        return DrawId.nullableOf(id);
    }

    // OutletId
    public UUID mapFromOutletId(OutletId id) {
        return id == null ? null : id.value();
    }

    public OutletId mapToOutletId(UUID id) {
        return OutletId.nullableOf(id);
    }

    // TerminalId
    public UUID mapFromTerminalId(TerminalId id) {
        return id == null ? null : id.value();
    }

    public TerminalId mapToTerminalId(UUID id) {
        return TerminalId.nullableOf(id);
    }

    // TicketId
    public UUID mapFromTicketId(TicketId id) {
        return id == null ? null : id.value();
    }

    public TicketId mapToTicketId(UUID id) {
        return TicketId.nullableOf(id);
    }

    // PayoutId
    public UUID mapFromPayoutId(PayoutId id) {
        return id == null ? null : id.value();
    }

    public PayoutId mapToPayoutId(UUID id) {
        return PayoutId.nullableOf(id);
    }

    // AgentId
    public UUID mapFromAgentId(AgentId id) {
        return id == null ? null : id.value();
    }

    public AgentId mapToAgentId(UUID id) {
        return AgentId.nullableOf(id);
    }

    // RoleId
    public UUID mapFromRoleId(RoleId id) {
        return id == null ? null : id.value();
    }

    public RoleId mapToRoleId(UUID id) {
        return RoleId.nullableOf(id);
    }

    // PlanId
    public UUID mapFromPlanId(PlanId id) {
        return id == null ? null : id.value();
    }

    public PlanId mapToPlanId(UUID id) {
        return PlanId.nullableOf(id);
    }

    // SessionId
    public UUID mapFromSessionId(SessionId id) {
        return id == null ? null : id.value();
    }

    public SessionId mapToSessionId(UUID id) {
        return SessionId.nullableOf(id);
    }

    // ThemePresetId
    public UUID mapFromThemePresetId(ThemePresetId id) {
        return id == null ? null : id.value();
    }

    public ThemePresetId mapToThemePresetId(UUID id) {
        return ThemePresetId.nullableOf(id);
    }

    // SettingId
    public UUID mapFromSettingId(SettingId id) {
        return id == null ? null : id.value();
    }

    public SettingId mapToSettingId(UUID id) {
        return SettingId.nullableOf(id);
    }

    // I18nOverrideId
    public UUID mapFromI18nOverrideId(I18nOverrideId id) {
        return id == null ? null : id.value();
    }

    public I18nOverrideId mapToI18nOverrideId(UUID id) {
        return I18nOverrideId.nullableOf(id);
    }

    // PageModelTemplateId
    public UUID mapFromPageModelTemplateId(PageModelTemplateId id) {
        return id == null ? null : id.value();
    }

    public PageModelTemplateId mapToPageModelTemplateId(UUID id) {
        return PageModelTemplateId.nullableOf(id);
    }

    // DrawChannelId
    public UUID mapFromDrawChannelId(DrawChannelId id) {
        return id == null ? null : id.value();
    }

    public DrawChannelId mapToDrawChannelId(UUID id) {
        return DrawChannelId.nullableOf(id);
    }

    // ResultSlotId
    public UUID mapFromResultSlotId(ResultSlotId id) {
        return id == null ? null : id.value();
    }

    public ResultSlotId mapToResultSlotId(UUID id) {
        return ResultSlotId.nullableOf(id);
    }

    // GameId
    public UUID mapFromGameId(GameId id) {
        return id == null ? null : id.value();
    }

    public GameId mapToGameId(UUID id) {
        return GameId.nullableOf(id);
    }

    // DrawChannelGameId
    public UUID mapFromDrawChannelGameId(DrawChannelGameId id) {
        return id == null ? null : id.value();
    }

    public DrawChannelGameId mapToDrawChannelGameId(UUID id) {
        return DrawChannelGameId.nullableOf(id);
    }

    // ZoneId
    public String mapFromZoneId(ZoneId zoneId) {
        return zoneId == null ? null : zoneId.getId();
    }

    public ZoneId mapToZoneId(String zoneId) {
        return (zoneId == null || zoneId.isBlank()) ? null : ZoneId.of(zoneId);
    }

    // DaysOfWeek (List<DayOfWeek> <-> String)
    public String mapFromDaysOfWeek(List<DayOfWeek> days) {
        return days == null ? null : DaysOfWeekFormatter.format(days);
    }

    public List<DayOfWeek> mapToDaysOfWeek(String days) {
        return (days == null || days.isBlank()) ? List.of() : DaysOfWeekFormatter.parse(days);
    }
}
