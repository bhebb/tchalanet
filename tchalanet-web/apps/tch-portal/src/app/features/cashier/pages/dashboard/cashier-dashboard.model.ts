export type DashboardUiState = 'loading' | 'ready' | 'partial' | 'error' | 'blocked';

export interface CashierDashboardView {
  readonly seller: CashierSellerView;
  readonly session: CashierSessionView;
  readonly kpis: CashierDashboardKpis;
  readonly currentDraw: CashierCurrentDrawView | null;
  readonly recentTickets: readonly CashierRecentTicketItem[];
  readonly printer: CashierPrinterStatusView | null;
  readonly notices?: readonly CashierDashboardNotice[];
}

export interface CashierSellerView {
  readonly displayName: string;
  readonly roleLabel: string;
  readonly terminalCode: string | null;
  readonly outletName: string | null;
}

export interface CashierSessionView {
  readonly status: 'OPEN' | 'CLOSED' | 'MISSING' | 'BLOCKED';
  readonly openedAt: string | null;
  readonly durationLabel: string | null;
}

export interface CashierDashboardKpis {
  readonly todaySalesAmount: number | null;
  readonly todaySalesCurrency: string;
  readonly commissionAmount: number | null;
  readonly commissionCurrency: string;
  readonly ticketCount: number | null;
}

export interface CashierCurrentDrawView {
  readonly label: string;
  readonly drawAt: string;
  readonly remainingSeconds: number | null;
  readonly previewNumbers?: readonly string[];
}

export interface CashierRecentTicketItem {
  readonly ticketId: string;
  readonly soldAt: string;
  readonly gameLabel: string;
  readonly amount: number;
  readonly currency: string;
  readonly status: 'PENDING' | 'WON' | 'LOST' | 'CANCELLED';
  readonly canPrint: boolean;
}

export interface CashierPrinterStatusView {
  readonly status: 'READY' | 'OFFLINE' | 'MISSING' | 'ERROR';
  readonly modelLabel: string | null;
}

export interface CashierDashboardNotice {
  readonly code: string;
  readonly severity: 'INFO' | 'WARN' | 'ERROR';
  readonly message: string;
}
