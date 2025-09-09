export type TchClaim = {
  tenantId: string;
  plan: string;
  featureSetId: string;
  locale: string;
  roles: string[];
};

export type UserSession = {
  id: string;
  username: string;
  email?: string;
};
