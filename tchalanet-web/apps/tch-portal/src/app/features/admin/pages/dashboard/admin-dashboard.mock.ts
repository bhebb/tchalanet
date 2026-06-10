import { AdminDashboardView } from './admin-dashboard.model';

export const ADMIN_DASHBOARD_MOCK: AdminDashboardView = {
  kpis: {
    sellers: 1284,
    outlets: 452,
    terminals: 890,
    openSessions: 312,
  },
  onboarding: {
    status: 'IN_PROGRESS',
    completedSteps: 2,
    totalSteps: 4,
  },
  recentSales: [
    { reference: '#983421', amount: 150, currency: 'HTG', createdAtLabel: '2 min ago' },
    { reference: '#983422', amount: 45, currency: 'HTG', createdAtLabel: '5 min ago' },
    { reference: '#983423', amount: 200, currency: 'HTG', createdAtLabel: '12 min ago' },
  ],
  attentionItems: [
    {
      id: 'terminal-offline',
      label: 'Terminal T-402 offline — Port-au-Prince Hub, no heartbeat for 12 min.',
      severity: 'ERROR',
    },
    {
      id: 'low-balance',
      label: 'Agent "J. Pascal" has reached 90% of credit limit.',
      severity: 'WARN',
    },
  ],
  notices: [],
};
