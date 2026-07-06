import { inject, Injectable } from '@angular/core';
import type { TchRequestOptions } from '@tch/api';
import { TchBackendClient, TchPage } from '@tch/api';
import type {
    ConsoleNotificationActorType,
    ConsoleNotificationAudienceType,
    ConsoleNotificationCategory,
    ConsoleNotificationChannel,
    ConsoleNotificationKind,
    ConsoleNotificationSeverity,
    ConsoleNotificationStatus,
} from '@tch/web/console';
import { Observable } from 'rxjs';

export type NotificationStatus = ConsoleNotificationStatus;
export type NotificationAudienceType = ConsoleNotificationAudienceType;
export type NotificationSeverity = ConsoleNotificationSeverity;
export type NotificationKind = ConsoleNotificationKind;
export type NotificationCategory = ConsoleNotificationCategory;
export type NotificationChannel = ConsoleNotificationChannel;
export type NotificationActorType = ConsoleNotificationActorType;

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
