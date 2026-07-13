import { WebAppError } from '@tch/api';

// ── Draw ───────────────────────────────────────────────────────────────────

export interface PosOpenDrawView {
  drawId: string;
  drawChannelId: string;
  drawDate: string;
  resultSlotKey: string;
  channelCode: string;
  channelLabel: string;
  gameCodes: string[];
  status: 'OPEN' | 'CLOSING_SOON' | string;
  scheduledAt: string;
  cutoffAt: string;
  /** Display label computed by the service (channelLabel). */
  label: string;
}

// ── Game ───────────────────────────────────────────────────────────────────

export interface PosBetOptionView {
  code: number;
  label: string;
  selectionHint?: string | null;
}

export interface PosGameBetTypeView {
  betType: string;
  label: string;
  requiresOption: boolean;
  selectionPolicy: PosSelectionPolicy;
  options: PosBetOptionView[];
  selectionHint?: string | null;
}

export type PosSelectionPolicy =
  | 'EXPLICIT_ONLY'
  | 'EXPLICIT_WITH_AUTO_OPTION'
  | 'IMPLICIT_BEST_MATCH'
  | string;

export interface PosGameView {
  gameCode: string;
  label: string;
  enabled: boolean;
  betType: string;
  betTypeLabel: string;
  requiresOption: boolean;
  selectionPolicy: PosSelectionPolicy;
  options: PosBetOptionView[];
  betTypes: PosGameBetTypeView[];
  selectionHint?: string | null;
}

// ── Ticket draft (local UI state) ──────────────────────────────────────────

export interface PosTicketDraftLine {
  localId: string;
  gameCode: string;
  selection: string;
  betType: string;
  betTypeLabel: string;
  betOption?: number | null;
  betOptionLabel?: string | null;
  stakeAmount: number;
}

export interface PosTicketLineInput {
  gameCode: string;
  selection: string;
  betType: string;
  betTypeLabel: string;
  betOption?: number | null;
  betOptionLabel?: string | null;
  stakeAmount: number;
}

// ── Prepared sale request ─────────────────────────────────────────────────

export interface ConfirmTicketSaleRequest {
  sellerTerminalId: string;
  drawId: string;
  drawChannelId?: string | null;
  currency: string;
  lines: ConfirmTicketSaleLineRequest[];
  promotionChoices?: null;
}

export interface ConfirmTicketSaleLineRequest {
  gameCode: string;
  betType: string;
  selection: string;
  betOption?: number | null;
  stake: number;
}

export interface PreparedTicketSaleView {
  preparationId: string;
  status: string;
  totalAmount: number;
  currency: string;
  freeLineCount: number;
  notices: readonly WebAppError[];
  canSell: boolean;
  actionAvailability: PosSaleActionAvailabilityView;
}

// ── Prepared sale confirmation response ───────────────────────────────────

export interface PosTicketBackupView {
  displayCode?: string | null;
  verificationShortUrl?: string | null;
  shareableText?: string | null;
}

export interface ConfirmedTicketView {
  outcome: 'ACCEPTED' | 'REJECTED' | 'PENDING_APPROVAL';
  ticketId: string;
  ticketCode: string;
  publicCode?: string | null;
  saleStatus?: string | null;
  backup?: PosTicketBackupView | null;
  sellerInstruction?: string | null;
  actionAvailability: PosSaleActionAvailabilityView;
  warnings: readonly WebAppError[];
}

export interface PosSaleActionAvailabilityView {
  canSell?: boolean;
  canPrint: boolean;
  canSendSms: boolean;
  canSendWhatsapp: boolean;
  canSendEmail: boolean;
  canCopy?: boolean;
}

// ── Seller terminal (for POS context) ─────────────────────────────────────

export interface PosSellerTerminalView {
  sellerTerminalId: string;
  terminalCode: string;
  displayName: string;
  status: 'ACTIVE' | 'BLOCKED' | 'INACTIVE' | 'DISABLED' | string;
  commissionRate?: number | null;
}

export interface PosSellerTerminalPickerView {
  sellerTerminalId: string;
  terminalCode: string;
  displayName: string;
  status: 'ACTIVE' | 'BLOCKED' | 'INACTIVE' | 'DISABLED' | string;
  commissionRate?: number | null;
  lastSeenAt?: string | null;
  todayTicketCount?: number | null;
  todaySalesAmount?: number | null;
}

export interface PosSellerTerminalListParams {
  q?: string;
  status?: string;
  page?: number;
  size?: number;
  sort?: string;
}

// ── Activity (matches SellerTerminalDailyStatsResponse on the server) ──────

export interface PosTerminalActivityView {
  ticketCount: number;
  /** Sales total in cents; divide by 100 for display. */
  salesTotalCents: number;
  currency: 'HTG';
}

// ── Ticket details / verification ─────────────────────────────────────────

export interface PosTicketDetailsView {
  id: string;
  ticketCode: string;
  publicCode: string;
  status: string;
  placedAt: string;
  cancelledAt?: string | null;
  drawId: string;
  drawChannelCode?: string | null;
  resultSlotKey?: string | null;
  resultProvider?: string | null;
  resultTimezone?: string | null;
  drawChannelName: string;
  drawScheduledAt: string;
  sellerTerminalId?: string | { value?: string | null } | null;
  outletName?: string | null;
  terminalCode?: string | null;
  sellerDisplayName?: string | null;
  lines: PosTicketDetailLineView[];
  stakeCents: number;
  totalAmountCents: number;
  currency: string;
  charges: PosTicketChargeView[];
}

export interface PosTicketDetailLineView {
  lineNumber: number;
  gameCode: string;
  gameLabel: string;
  betType: string;
  betTypeLabel: string;
  selection: string;
  stakeAmountCents: number;
  promotional: boolean;
  promotionLabel?: string | null;
}

export interface PosTicketChargeView {
  type: string;
  label: string;
  amountCents: number;
  waived: boolean;
  waivedLabel?: string | null;
}

export interface PosTicketVerificationView {
  status: string;
  severity: 'SUCCESS' | 'INFO' | 'WARNING' | 'ERROR' | string;
  titleKey: string;
  messageKey: string;
  params: Record<string, unknown>;
  availableActions: PosTicketVerificationActionView[];
}

export interface PosTicketVerificationActionView {
  type: string;
  labelKey: string;
  enabled: boolean;
  params: Record<string, unknown>;
}
