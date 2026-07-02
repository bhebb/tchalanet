import { WebAppError } from '@tch/api';

// ── Draw ───────────────────────────────────────────────────────────────────

export interface PosOpenDrawView {
  drawId: string;
  drawChannelId: string;
  drawDate: string;
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
  options: PosBetOptionView[];
  selectionHint?: string | null;
}

export interface PosGameView {
  gameCode: string;
  label: string;
  enabled: boolean;
  betType: string;
  betTypeLabel: string;
  requiresOption: boolean;
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
  stakeAmount: number;
}

export interface PosTicketLineInput {
  gameCode: string;
  selection: string;
  betType: string;
  betTypeLabel: string;
  stakeAmount: number;
}

// ── Sell request (matches PosSellTicketRequest on the server) ──────────────

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

// ── Sale preview (server-side read-only validation) ───────────────────────

export interface PreviewTicketSaleView {
  decision: 'ACCEPTABLE' | 'REQUIRES_CHANGES' | 'REJECTED_FINAL' | string;
  sellerInstruction?: string | null;
  warning?: string | null;
  issues: PreviewTicketSaleIssueView[];
  notices: readonly WebAppError[];
  canSell: boolean;
}

export interface PreviewTicketSaleIssueView {
  code: string;
  severity: string;
  message?: string | null;
  sellerInstruction?: string | null;
  lineIndex: number;
}

// ── Sell response (matches PosSellTicketResponse on the server) ────────────

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
  warnings: readonly WebAppError[];
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
