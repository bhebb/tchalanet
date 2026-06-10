import { PlatformDashboardView } from './platform-dashboard.model';

export const PLATFORM_DASHBOARD_MOCK: PlatformDashboardView = {
  kpis: {
    activeTenants: 1284,
    activeTenantsTrendPercent: 4,
    receivedContacts: 156,
    pendingNotifications: 28,
    degradedServices: 2,
  },
  provisioning: [
    {
      tenantName: 'Global Lotto Systems',
      tenantCode: 'GLS-992-HT',
      progressPercent: 85,
      status: 'HEALTHY',
    },
    {
      tenantName: 'Port-au-Prince Digital',
      tenantCode: 'PAP-401-LD',
      progressPercent: 42,
      status: 'PROVISIONING',
    },
    {
      tenantName: 'Nord-Est Betting Network',
      tenantCode: 'NEB-112-XG',
      progressPercent: 12,
      status: 'PENDING',
    },
  ],
  operationalIntegrity: {
    scorePercent: 98.4,
    lastAuditLabel: 'Dernier audit il y a 4h',
    status: 'READY',
    message: 'Les services critiques sont opérationnels.',
  },
  recentContactRequests: [
    {
      reference: 'CT-26-000123',
      requesterName: 'Jean Dupond',
      requesterLabel: 'PAP Master Agent',
      subject: 'Demande accès API',
      createdAtLabel: '24 oct., 09:42',
      status: 'PENDING',
    },
    {
      reference: 'CT-26-000124',
      requesterName: 'Marie Laurent',
      requesterLabel: 'Cap-Haïtien Retailer',
      subject: 'Provisioning nouveau terminal',
      createdAtLabel: '24 oct., 08:15',
      status: 'PROCESSING',
    },
  ],
  notices: [],
};
