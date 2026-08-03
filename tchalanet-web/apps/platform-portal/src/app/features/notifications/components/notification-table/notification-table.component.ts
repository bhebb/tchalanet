import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import {
    AdminEmptyStateComponent,
    AdminStatusPillComponent,
    AdminStatusTone,
} from '@tch/ui/console';
import { ErrorViewModel } from '@tch/web/errors';

import {
    NotificationItemView,
    NotificationSeverity,
    NotificationStatus,
} from '../../data-access/notifications-api.service';

@Component({
    selector: 'tch-notification-table',
    imports: [
        DatePipe,
        AdminEmptyStateComponent,
        AdminStatusPillComponent,
        TchErrorPanel,
        TchLoading,
        MatButtonModule,
        MatIconModule,
        MatTableModule,
        MatTooltipModule,
    ],
    templateUrl: './notification-table.component.html',
    styleUrl: './notification-table.component.scss',
})
export class NotificationTableComponent {
    readonly notifications = input<NotificationItemView[]>([]);
    readonly loading = input<boolean>(false);
    readonly error = input<ErrorViewModel | null>(null);

    readonly publish = output<NotificationItemView>();
    readonly republish = output<NotificationItemView>();
    readonly replayRecipients = output<NotificationItemView>();
    readonly markRead = output<NotificationItemView>();
    readonly cancel = output<NotificationItemView>();
    readonly archive = output<NotificationItemView>();
    readonly openTranslations = output<NotificationItemView>();
    readonly retry = output<void>();

    readonly displayedColumns = ['createdAt', 'severity', 'title', 'category', 'status', 'actions'];

    idOf(item: NotificationItemView): string {
        return typeof item.id === 'string' ? item.id : item.id.value;
    }

    titleOf(item: NotificationItemView): string {
        return item.titleText || item.titleKey || 'Notification';
    }

    messageOf(item: NotificationItemView): string {
        return item.messageText || item.messageKey || '';
    }

    systemKeysOf(item: NotificationItemView): string[] {
        return [item.titleKey, item.messageKey].filter((key): key is string => !!key);
    }

    hasSystemKeys(item: NotificationItemView): boolean {
        return this.systemKeysOf(item).length > 0;
    }

    languageModeOf(item: NotificationItemView): string {
        return this.hasSystemKeys(item) ? 'System keys' : 'FR / EN / HT';
    }

    severityTone(severity: NotificationSeverity): AdminStatusTone {
        if (severity === 'CRITICAL' || severity === 'ERROR') return 'danger';
        if (severity === 'WARNING') return 'warning';
        return 'info';
    }

    statusTone(status: NotificationStatus): AdminStatusTone {
        if (status === 'PUBLISHED') return 'success';
        if (status === 'EXPIRED') return 'warning';
        return 'neutral';
    }

    statusLabel(status: NotificationStatus): string {
        return ({
            PUBLISHED: 'Publiée',
            EXPIRED: 'Expiré',
            CANCELLED: 'Annulée',
            PURGED: 'Purgée',
        } as Record<NotificationStatus, string>)[status];
    }

    canPublish(item: NotificationItemView): boolean {
        return item.status === 'EXPIRED';
    }

    canRepublish(item: NotificationItemView): boolean {
        return item.status === 'PUBLISHED' || item.status === 'EXPIRED';
    }

    canCancel(item: NotificationItemView): boolean {
        return item.status === 'PUBLISHED' || item.status === 'EXPIRED';
    }
}
