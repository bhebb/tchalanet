package com.tchalanet.server.common.mapper;

import com.tchalanet.server.common.time.DaysOfWeekFormatter;
import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.common.types.id.DrawChannelGameId;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.types.id.I18nOverrideId;
import com.tchalanet.server.common.types.id.LedgerEntryId;
import com.tchalanet.server.common.types.id.NotificationDeliveryId;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.types.id.NotificationPreferenceId;
import com.tchalanet.server.common.types.id.OfflineSyncBatchId;
import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineCodeId;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.PromotionAttemptId;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.common.types.id.SalesZoneId;
import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.SellerOutletAssignmentId;
import com.tchalanet.server.common.types.id.SellerCommissionPolicyId;
import com.tchalanet.server.common.types.id.AppliedPromotionId;
import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.common.types.id.PromotionQuotaId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.PlanId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.SettingId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.ThemePresetId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.id.ReconciliationRunId;
import com.tchalanet.server.common.types.id.ReconciliationCheckResultId;
import com.tchalanet.server.common.types.id.ReconciliationAnomalyId;
import com.tchalanet.server.common.types.id.ReconciliationRepairActionId;
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

    public UUID mapFromNotificationId(NotificationId id) {
        return id == null ? null : id.value();
    }

    public NotificationId mapToNotificationId(UUID id) {
        return NotificationId.nullableOf(id);
    }

    public UUID mapFromNotificationDeliveryId(NotificationDeliveryId id) {
        return id == null ? null : id.value();
    }

    public NotificationDeliveryId mapToNotificationDeliveryId(UUID id) {
        return NotificationDeliveryId.nullableOf(id);
    }

    public UUID mapFromNotificationPreferenceId(NotificationPreferenceId id) {
        return id == null ? null : id.value();
    }

    public NotificationPreferenceId mapToNotificationPreferenceId(UUID id) {
        return NotificationPreferenceId.nullableOf(id);
    }

    // DrawId
    public UUID mapFromDrawId(DrawId id) {
        return id == null ? null : id.value();
    }

    public DrawId mapToDrawId(UUID id) {
        return DrawId.nullableOf(id);
    }// DrawId

    public UUID mapFromDrawResultId(DrawResultId id) {
        return id == null ? null : id.value();
    }

    public DrawResultId mapToDrawIResultd(UUID id) {
        return DrawResultId.nullableOf(id);
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

    // LedgerEntryId
    public UUID mapFromLedgerEntryId(LedgerEntryId id) {
        return id == null ? null : id.value();
    }

    public LedgerEntryId mapToLedgerEntryId(UUID id) {
        return LedgerEntryId.nullableOf(id);
    }

    // ApprovalRequestId
    public UUID mapFromApprovalRequestId(ApprovalRequestId id) {
        return id == null ? null : id.value();
    }

    public ApprovalRequestId mapToApprovalRequestId(UUID id) {
        return ApprovalRequestId.nullableOf(id);
    }

    // OfflineSyncBatchId
    public UUID mapFromOfflineSyncBatchId(OfflineSyncBatchId id) {
        return id == null ? null : id.value();
    }

    public OfflineSyncBatchId mapToOfflineSyncBatchId(UUID id) {
        return OfflineSyncBatchId.nullableOf(id);
    }

    // OfflineCodeBatchId
    public UUID mapFromOfflineCodeBatchId(OfflineCodeBatchId id) {
        return id == null ? null : id.value();
    }

    public OfflineCodeBatchId mapToOfflineCodeBatchId(UUID id) {
        return OfflineCodeBatchId.nullableOf(id);
    }

    // OfflineCodeId
    public UUID mapFromOfflineCodeId(OfflineCodeId id) {
        return id == null ? null : id.value();
    }

    public OfflineCodeId mapToOfflineCodeId(UUID id) {
        return OfflineCodeId.nullableOf(id);
    }

    // OfflineCodeId
    public UUID mapFromOfflineGrantId(OfflineGrantId id) {
        return id == null ? null : id.value();
    }

    public OfflineGrantId mapToOfflineGrantId(UUID id) {
        return OfflineGrantId.nullableOf(id);
    }

    // OfflineCodeId
    public UUID mapFromOfflineSubmissionId(OfflineSubmissionId id) {
        return id == null ? null : id.value();
    }

    public OfflineSubmissionId mapToOfflineSubmissionId(UUID id) {
        return OfflineSubmissionId.nullableOf(id);
    }

    // PromotionAttemptId
    public UUID mapFromPromotionAttemptId(PromotionAttemptId id) {
        return id == null ? null : id.value();
    }

    public PromotionAttemptId mapToPromotionAttemptId(UUID id) {
        return PromotionAttemptId.nullableOf(id);
    }

    // PromotionRuleId
    public UUID mapFromPromotionRuleId(PromotionRuleId id) {
        return id == null ? null : id.value();
    }

    public PromotionRuleId mapToPromotionRuleId(UUID id) {
        return PromotionRuleId.nullableOf(id);
    }

    // SellerId
    public UUID mapFromSellerId(SellerId id) {
        return id == null ? null : id.value();
    }

    public SellerId mapToSellerId(UUID id) {
        return SellerId.nullableOf(id);
    }

    // SellerOutletAssignmentId
    public UUID mapFromSellerOutletAssignmentId(SellerOutletAssignmentId id) {
        return id == null ? null : id.value();
    }

    public SellerOutletAssignmentId mapToSellerOutletAssignmentId(UUID id) {
        return SellerOutletAssignmentId.nullableOf(id);
    }

    // SellerCommissionPolicyId
    public UUID mapFromSellerCommissionPolicyId(SellerCommissionPolicyId id) {
        return id == null ? null : id.value();
    }

    public SellerCommissionPolicyId mapToSellerCommissionPolicyId(UUID id) {
        return SellerCommissionPolicyId.nullableOf(id);
    }

    // AppliedPromotionId
    public UUID mapFromAppliedPromotionId(AppliedPromotionId id) {
        return id == null ? null : id.value();
    }

    public AppliedPromotionId mapToAppliedPromotionId(UUID id) {
        return AppliedPromotionId.nullableOf(id);
    }

    // PromotionDecisionId
    public UUID mapFromPromotionDecisionId(PromotionDecisionId id) {
        return id == null ? null : id.value();
    }

    public PromotionDecisionId mapToPromotionDecisionId(UUID id) {
        return PromotionDecisionId.nullableOf(id);
    }

    // PromotionCampaignId
    public UUID mapFromPromotionCampaignId(PromotionCampaignId id) {
        return id == null ? null : id.value();
    }

    public PromotionCampaignId mapToPromotionCampaignId(UUID id) {
        return PromotionCampaignId.nullableOf(id);
    }

    // PromotionQuotaId
    public UUID mapFromPromotionQuotaId(PromotionQuotaId id) {
        return id == null ? null : id.value();
    }

    public PromotionQuotaId mapToPromotionQuotaId(UUID id) {
        return PromotionQuotaId.nullableOf(id);
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
    public UUID mapFromSessionId(SalesSessionId id) {
        return id == null ? null : id.value();
    }

    public SalesSessionId mapToSessionId(UUID id) {
        return SalesSessionId.nullableOf(id);
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
    public UUID mapFromTicketLineId(TicketLineId id) {
        return id == null ? null : id.value();
    }

    public TicketLineId mapToTicketLineId(UUID id) {
        return TicketLineId.nullableOf(id);
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

    // SalesZoneId
    public UUID mapFromSalesZoneId(SalesZoneId id) {
        return id == null ? null : id.value();
    }

    public SalesZoneId mapToSalesZoneId(UUID id) {
        return SalesZoneId.nullableOf(id);
    }

    // ZoneId (java.time.ZoneId — timezone, not sales zone)
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

    // Reconciliation IDs
    public UUID mapFromReconciliationRunId(ReconciliationRunId id) {
        return id == null ? null : id.value();
    }

    public ReconciliationRunId mapToReconciliationRunId(UUID id) {
        return ReconciliationRunId.nullableOf(id);
    }

    public UUID mapFromReconciliationCheckResultId(ReconciliationCheckResultId id) {
        return id == null ? null : id.value();
    }

    public ReconciliationCheckResultId mapToReconciliationCheckResultId(UUID id) {
        return ReconciliationCheckResultId.nullableOf(id);
    }

    public UUID mapFromReconciliationAnomalyId(ReconciliationAnomalyId id) {
        return id == null ? null : id.value();
    }

    public ReconciliationAnomalyId mapToReconciliationAnomalyId(UUID id) {
        return ReconciliationAnomalyId.nullableOf(id);
    }

    public UUID mapFromReconciliationRepairActionId(ReconciliationRepairActionId id) {
        return id == null ? null : id.value();
    }

    public ReconciliationRepairActionId mapToReconciliationRepairActionId(UUID id) {
        return ReconciliationRepairActionId.nullableOf(id);
    }
}
