import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';

export type PrivateNotificationSeverity = 'INFO' | 'WARNING' | 'ERROR' | 'CRITICAL';
export type PrivateNotificationStatus = 'DRAFT' | 'PUBLISHED' | 'EXPIRED' | 'CANCELLED' | 'PURGED';

export interface PrivateNotificationPage<T> {
  readonly items?: T[];
  readonly content?: T[];
  readonly totalElements: number;
  readonly totalPages: number;
  readonly number?: number;
  readonly size?: number;
}

export interface PrivateNotificationAction {
  readonly type: string | null;
  readonly url: string | null;
  readonly labelKey: string | null;
}

export interface PrivateNotificationItem {
  readonly id: string | { readonly value: string };
  readonly severity: PrivateNotificationSeverity;
  readonly kind: string;
  readonly category: string;
  readonly titleKey: string | null;
  readonly messageKey: string | null;
  readonly titleText: string | null;
  readonly messageText: string | null;
  readonly action: PrivateNotificationAction | null;
  readonly status: PrivateNotificationStatus;
  readonly readAt: string | null;
  readonly archivedAt: string | null;
  readonly expiresAt: string | null;
  readonly createdAt: string;
}

export interface PrivateNotificationUnreadCount {
  readonly unreadCount: number;
}

@Injectable({ providedIn: 'root' })
export class PrivateNotificationsApi {
  private readonly backend = inject(TchBackendClient);

  listLatest(scope: PrivateNotificationScope, size = 5): Observable<PrivateNotificationPage<PrivateNotificationItem>> {
    return this.backend.get<PrivateNotificationPage<PrivateNotificationItem>>(
      `${this.basePath(scope)}?page=0&size=${size}`,
    );
  }

  unreadCount(scope: PrivateNotificationScope): Observable<PrivateNotificationUnreadCount> {
    return this.backend.get<PrivateNotificationUnreadCount>(`${this.basePath(scope)}/unread-count`);
  }

  markRead(scope: PrivateNotificationScope, id: string): Observable<boolean> {
    return this.backend.post<boolean>(`${this.basePath(scope)}/${id}/read`, {});
  }

  dismiss(scope: PrivateNotificationScope, id: string): Observable<boolean> {
    return this.backend.post<boolean>(`${this.basePath(scope)}/${id}/dismiss`, {});
  }

  markAllRead(scope: PrivateNotificationScope): Observable<boolean> {
    return this.backend.post<boolean>(`${this.basePath(scope)}/read-all`, {});
  }

  private basePath(scope: PrivateNotificationScope): string {
    return scope === 'platform' ? '/platform/notifications' : '/admin/notifications';
  }
}

export type PrivateNotificationScope = 'platform' | 'admin';
