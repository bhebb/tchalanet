export type ContactIntent =
  | 'REQUEST_DEMO'
  | 'BECOME_OPERATOR'
  | 'SUPPORT'
  | 'PARTNERSHIP'
  | 'OTHER';

export interface SubmitContactRequest {
  readonly intent: ContactIntent;
  readonly fullName: string;
  readonly phone: string;
  readonly email?: string;
  readonly organizationName?: string;
  readonly city?: string;
  readonly country?: string;
  readonly outletCount?: number;
  readonly preferredContactTime?: string;
  readonly message: string;
  readonly consentToContact: boolean;
  readonly sourcePage?: string;
}

export interface ContactRequestSubmittedResponse {
  readonly requestId: string;
  readonly status: 'RECEIVED';
  readonly message: string;
}

export const CONTACT_INTENTS: readonly ContactIntent[] = [
  'REQUEST_DEMO',
  'BECOME_OPERATOR',
  'SUPPORT',
  'PARTNERSHIP',
  'OTHER',
];
