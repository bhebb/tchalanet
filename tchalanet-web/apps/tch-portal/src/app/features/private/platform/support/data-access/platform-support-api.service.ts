import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import type { TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';

export type ContactRequestIntent =
  | 'REQUEST_DEMO'
  | 'BECOME_OPERATOR'
  | 'SUPPORT'
  | 'PARTNERSHIP'
  | 'OTHER';

export type ContactRequestStatus =
  | 'RECEIVED'
  | 'CONTACTED'
  | 'QUALIFIED'
  | 'CLOSED'
  | 'SPAM';

export interface TchPage<T> {
  items: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ContactRequestSummaryView {
  id: string;
  reference: string;
  intent: ContactRequestIntent;
  fullName: string;
  phone: string | null;
  email: string | null;
  city: string | null;
  country: string | null;
  status: ContactRequestStatus;
  createdAt: string;
}

export interface ContactRequestAdminDetailView extends ContactRequestSummaryView {
  organizationName: string | null;
  outletCount: number | null;
  preferredContactTime: string | null;
  message: string | null;
  consentToContact: boolean;
  internalNotes: string | null;
  externalTool: string | null;
  externalReference: string | null;
  exportedAt: string | null;
  sourcePage: string | null;
  updatedAt: string;
}

export type PublicContentSourceType = 'INTERNAL' | 'EXTERNAL_RSS';
export type PublicContentStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
export type PublicContentSurface =
  | 'PUBLIC_HOME'
  | 'TENANT_ADMIN_DASHBOARD'
  | 'PLATFORM_ADMIN_DASHBOARD'
  | 'POS_DASHBOARD';

export interface PublicContentAdminItemView {
  id: string;
  title: string;
  content: string | null;
  imageUrl: string | null;
  sourceUrl: string | null;
  sourceType: PublicContentSourceType;
  status: PublicContentStatus;
  publishedAt: string | null;
  expiresAt: string | null;
  targetSurfaces: PublicContentSurface[];
  createdBy: string | null;
  createdAt: string | null;
  lastModifiedBy: string | null;
  lastModifiedAt: string | null;
}

export interface UpsertPublicContentRequest {
  id?: string | null;
  title: string;
  content?: string | null;
  contentHtml?: string | null;
  imageUrl?: string | null;
  sourceUrl?: string | null;
  status?: PublicContentStatus | null;
  targetSurfaces?: PublicContentSurface[];
  publishedAt?: string | null;
  expiresAt?: string | null;
}

export type NotificationStatus = 'PUBLISHED' | 'EXPIRED' | 'CANCELLED' | 'PURGED';
export type NotificationAudienceType =
  | 'SPECIFIC_ACTORS'
  | 'PLATFORM_ADMINS'
  | 'ALL_APP_USERS'
  | 'TENANT_ADMINS'
  | 'TENANT_APP_USERS'
  | 'TENANT_SELLER_TERMINALS';
export type NotificationSeverity = 'INFO' | 'WARNING' | 'ERROR' | 'CRITICAL';
export type NotificationKind = 'INFO' | 'WARNING' | 'ACTION_REQUIRED' | 'SYSTEM_ERROR';
export type NotificationCategory =
  | 'PAGE_MODEL'
  | 'TENANT_CONFIG'
  | 'USER'
  | 'OUTLET'
  | 'TERMINAL'
  | 'SESSION'
  | 'SALES'
  | 'DRAW'
  | 'RESULT'
  | 'PAYOUT'
  | 'BATCH'
  | 'SYSTEM'
  | 'SECURITY';
export type NotificationChannel = 'EMAIL' | 'SLACK' | 'SMS' | 'WHATSAPP' | 'PUSH' | 'WEB' | 'IN_APP';
export type NotificationActorType = 'APP_USER' | 'SELLER_TERMINAL';

export interface NotificationTarget {
  actorType: NotificationActorType;
  actorId: string;
}

export interface NotificationItemView {
  id: string | { value: string };
  severity: NotificationSeverity;
  kind: NotificationKind;
  category: NotificationCategory;
  titleKey: string | null;
  messageKey: string | null;
  titleText: string | null;
  messageText: string | null;
  payload: unknown;
  action: { type: string | null; url: string | null; labelKey: string | null } | null;
  status: NotificationStatus;
  readAt: string | null;
  archivedAt: string | null;
  expiresAt: string | null;
  createdAt: string;
}

export interface CreateNotificationRequest {
  sourceType?: string | null;
  sourceId?: string | null;
  dedupeKey?: string | null;
  audienceType: NotificationAudienceType;
  targets?: NotificationTarget[];
  severity: NotificationSeverity;
  kind: NotificationKind;
  category: NotificationCategory;
  titleText?: string | null;
  messageText?: string | null;
  translations?: Record<'fr' | 'en' | 'ht', { title: string; body: string }>;
  payload?: Record<string, unknown> | null;
  actionType?: string | null;
  actionUrl?: string | null;
  expiresAt?: string | null;
  channels: NotificationChannel[];
}

@Injectable({ providedIn: 'root' })
export class PlatformSupportApi {
  private readonly backend = inject(TchBackendClient);

  listContactRequests(params: {
    q?: string;
    status?: ContactRequestStatus;
    intent?: ContactRequestIntent;
    page?: number;
    size?: number;
  }, options?: TchRequestOptions): Observable<TchPage<ContactRequestSummaryView>> {
    const entries = Object.entries(params)
      .filter(([, value]) => value !== undefined && value !== null && String(value) !== '')
      .map(([key, value]) => [key, String(value)]);
    const q = new URLSearchParams(Object.fromEntries(entries)).toString();

    return this.backend.get<TchPage<ContactRequestSummaryView>>(
      `/platform/contact-requests${q ? '?' + q : ''}`,
      options,
    );
  }

  getContactRequest(id: string, options?: TchRequestOptions): Observable<ContactRequestAdminDetailView> {
    return this.backend.get<ContactRequestAdminDetailView>(`/platform/contact-requests/${id}`, options);
  }

  updateContactStatus(id: string, status: ContactRequestStatus, options?: TchRequestOptions): Observable<void> {
    return this.backend.patch<void>(`/platform/contact-requests/${id}/status`, { status }, options);
  }

  updateContactNotes(
    id: string,
    payload: {
      internalNotes: string | null;
      externalTool?: string | null;
      externalReference?: string | null;
    },
    options?: TchRequestOptions,
  ): Observable<void> {
    return this.backend.patch<void>(`/platform/contact-requests/${id}/notes`, payload, options);
  }

  listNews(options?: TchRequestOptions): Observable<PublicContentAdminItemView[]> {
    return this.backend.get<PublicContentAdminItemView[]>('/platform/public-content/news', options);
  }

  upsertNews(
    payload: UpsertPublicContentRequest,
    options?: TchRequestOptions,
  ): Observable<PublicContentAdminItemView> {
    return this.backend.post<PublicContentAdminItemView>('/platform/public-content/news', payload, options);
  }

  changeNewsStatus(
    id: string,
    status: PublicContentStatus,
    options?: TchRequestOptions,
  ): Observable<PublicContentAdminItemView> {
    return this.backend.post<PublicContentAdminItemView>(
      `/platform/public-content/news/${id}/status`,
      { status },
      options,
    );
  }

  hideNews(id: string, options?: TchRequestOptions): Observable<void> {
    return this.backend.post<void>(`/platform/public-content/news/${id}/hide`, {}, options);
  }

  showNews(id: string, options?: TchRequestOptions): Observable<void> {
    return this.backend.post<void>(`/platform/public-content/news/${id}/show`, {}, options);
  }

  forceRefreshNews(options?: TchRequestOptions): Observable<void> {
    return this.backend.post<void>('/platform/public-content/news/force-refresh', {}, options);
  }

  listNotifications(params: {
    q?: string;
    status?: NotificationStatus;
    category?: NotificationCategory;
    kind?: NotificationKind;
    severity?: NotificationSeverity;
    page?: number;
    size?: number;
  }, options?: TchRequestOptions): Observable<TchPage<NotificationItemView>> {
    const entries = Object.entries(params)
      .filter(([, value]) => value !== undefined && value !== null && String(value) !== '')
      .map(([key, value]) => [key, String(value)]);
    const q = new URLSearchParams(Object.fromEntries(entries)).toString();
    return this.backend.get<TchPage<NotificationItemView>>(`/platform/notifications${q ? '?' + q : ''}`, options);
  }

  createNotification(payload: CreateNotificationRequest, options?: TchRequestOptions): Observable<boolean> {
    return this.backend.post<boolean>('/platform/notifications', payload, options);
  }

  createTenantNotification(
    tenantId: string,
    payload: CreateNotificationRequest,
    options?: TchRequestOptions,
  ): Observable<boolean> {
    return this.backend.post<boolean>(`/platform/tenants/${tenantId}/notifications`, payload, options);
  }

  markNotificationRead(id: string, options?: TchRequestOptions): Observable<boolean> {
    return this.backend.post<boolean>(`/platform/notifications/${id}/read`, {}, options);
  }

  archiveNotification(id: string, options?: TchRequestOptions): Observable<boolean> {
    return this.backend.post<boolean>(`/platform/notifications/${id}/archive`, {}, options);
  }

  publishNotification(id: string, reason?: string | null, options?: TchRequestOptions): Observable<unknown> {
    return this.backend.post<unknown>(`/platform/notifications/${id}/publish`, { reason: reason ?? null }, options);
  }

  republishNotification(id: string, reason: string, options?: TchRequestOptions): Observable<unknown> {
    return this.backend.post<unknown>(`/platform/notifications/${id}/republish`, { reason }, options);
  }

  replayNotificationRecipients(id: string, options?: TchRequestOptions): Observable<number> {
    return this.backend.post<number>(`/platform/notifications/${id}/replay-recipients`, {}, options);
  }

  cancelNotification(id: string, reason: string, options?: TchRequestOptions): Observable<boolean> {
    return this.backend.post<boolean>(`/platform/notifications/${id}/cancel`, { reason }, options);
  }

  purgeExpiredNotifications(dryRun: boolean, options?: TchRequestOptions): Observable<unknown> {
    return this.backend.post<unknown>('/platform/notifications/purge-expired', { dryRun }, options);
  }
}
