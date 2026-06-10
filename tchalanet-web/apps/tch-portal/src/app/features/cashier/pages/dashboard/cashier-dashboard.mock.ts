import { CashierDashboardView } from './cashier-dashboard.model';

export const CASHIER_DASHBOARD_MOCK: CashierDashboardView = {
  seller: {
    displayName: 'Jean Dupont',
    roleLabel: 'Vendeur Senior',
    terminalCode: '#77421',
    outletName: 'Port-au-Prince Centre',
  },
  session: {
    status: 'OPEN',
    openedAt: '07:45',
    durationLabel: '04h 32m',
  },
  kpis: {
    todaySalesAmount: 45280,
    todaySalesCurrency: 'HTG',
    commissionAmount: 3140,
    commissionCurrency: 'HTG',
    ticketCount: 142,
  },
  currentDraw: {
    label: 'Tirage New York — Midi',
    drawAt: '12:30',
    remainingSeconds: 2538,
  },
  recentTickets: [
    { ticketId: '#TC-882941', soldAt: '11:42', gameLabel: 'Borlette New York', amount: 250, currency: 'HTG', status: 'WON', canPrint: true },
    { ticketId: '#TC-882942', soldAt: '11:38', gameLabel: 'Lotto 3 Chiffres', amount: 100, currency: 'HTG', status: 'PENDING', canPrint: true },
    { ticketId: '#TC-882943', soldAt: '11:30', gameLabel: 'Marriage', amount: 500, currency: 'HTG', status: 'LOST', canPrint: false },
    { ticketId: '#TC-882944', soldAt: '11:15', gameLabel: 'Borlette Midi', amount: 150, currency: 'HTG', status: 'WON', canPrint: true },
  ],
  printer: {
    status: 'READY',
    modelLabel: 'Epson TM-T88VI',
  },
  notices: [],
};
