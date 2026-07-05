import { inject, Injectable } from '@angular/core';
import type { TchRequestOptions } from '@tch/api';
import { TchBackendClient, TchPage } from '@tch/api';
import { Observable } from 'rxjs';

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

@Injectable({providedIn: 'root'})
export class NotificationsApi {
    private readonly backend = inject(TchBackendClient);


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
        return this.backend.post<unknown>(`/platform/notifications/${id}/publish`, {reason: reason ?? null}, options);
    }

    republishNotification(id: string, reason: string, options?: TchRequestOptions): Observable<unknown> {
        return this.backend.post<unknown>(`/platform/notifications/${id}/republish`, {reason}, options);
    }

    replayNotificationRecipients(id: string, options?: TchRequestOptions): Observable<number> {
        return this.backend.post<number>(`/platform/notifications/${id}/replay-recipients`, {}, options);
    }

    cancelNotification(id: string, reason: string, options?: TchRequestOptions): Observable<boolean> {
        return this.backend.post<boolean>(`/platform/notifications/${id}/cancel`, {reason}, options);
    }

    purgeExpiredNotifications(dryRun: boolean, options?: TchRequestOptions): Observable<unknown> {
        return this.backend.post<unknown>('/platform/notifications/purge-expired', {dryRun}, options);
    }
}
