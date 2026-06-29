import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';

export interface AdminNotificationsPage<T> {
  readonly items?: T[];
  readonly content?: T[];
  readonly totalElements: number;
  readonly totalPages: number;
  readonly number?: number;
  readonly size?: number;
}

export type AdminNotificationSeverity = 'INFO' | 'WARNING' | 'ERROR' | 'CRITICAL';
export type AdminNotificationCategory =
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

export interface AdminNotificationItem {
  readonly id: string | { readonly value: string };
  readonly severity: AdminNotificationSeverity;
  readonly category: AdminNotificationCategory;
  readonly titleKey: string | null;
  readonly messageKey: string | null;
  readonly titleText: string | null;
  readonly messageText: string | null;
  readonly readAt: string | null;
  readonly archivedAt: string | null;
  readonly expiresAt: string | null;
  readonly createdAt: string;
}

export interface AdminNotificationUnreadCount {
  readonly unreadCount: number;
}

export interface CreateAdminNotificationRequest {
  readonly audienceType: 'TENANT_ADMINS' | 'TENANT_APP_USERS' | 'TENANT_SELLER_TERMINALS';
  readonly severity: AdminNotificationSeverity;
  readonly kind: 'INFO' | 'WARNING' | 'ACTION_REQUIRED' | 'SYSTEM_ERROR';
  readonly category: AdminNotificationCategory;
  readonly titleText: string;
  readonly messageText: string;
  readonly translations: {
    readonly fr: { readonly title: string; readonly body: string };
    readonly en: { readonly title: string; readonly body: string };
    readonly ht: { readonly title: string; readonly body: string };
  };
  readonly actionUrl?: string | null;
  readonly payload?: Record<string, unknown> | null;
  readonly channels: ('IN_APP' | 'EMAIL' | 'SMS' | 'WHATSAPP')[];
}

@Injectable({ providedIn: 'root' })
export class AdminNotificationsApi {
  private readonly backend = inject(TchBackendClient);

  list(params: {
    readonly severity?: AdminNotificationSeverity;
    readonly category?: AdminNotificationCategory;
    readonly page?: number;
    readonly size?: number;
  }, options?: TchRequestOptions): Observable<AdminNotificationsPage<AdminNotificationItem>> {
    const entries = Object.entries(params)
      .filter(([, value]) => value !== undefined && value !== null && String(value) !== '')
      .map(([key, value]) => [key, String(value)]);
    const q = new URLSearchParams(Object.fromEntries(entries)).toString();
    return this.backend.get<AdminNotificationsPage<AdminNotificationItem>>(
      `/admin/notifications${q ? '?' + q : ''}`,
      options,
    );
  }

  unreadCount(options?: TchRequestOptions): Observable<AdminNotificationUnreadCount> {
    return this.backend.get<AdminNotificationUnreadCount>('/admin/notifications/unread-count', options);
  }

  create(payload: CreateAdminNotificationRequest, options?: TchRequestOptions): Observable<boolean> {
    return this.backend.post<boolean>('/admin/notifications', payload, options);
  }

  markRead(id: string, options?: TchRequestOptions): Observable<boolean> {
    return this.backend.post<boolean>(`/admin/notifications/${id}/read`, {}, options);
  }

  dismiss(id: string, options?: TchRequestOptions): Observable<boolean> {
    return this.backend.post<boolean>(`/admin/notifications/${id}/dismiss`, {}, options);
  }

  markAllRead(options?: TchRequestOptions): Observable<boolean> {
    return this.backend.post<boolean>('/admin/notifications/read-all', {}, options);
  }
}
