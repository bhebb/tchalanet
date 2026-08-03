import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import {
    AdminListStatusOption,
    AdminListSurface,
    TchConfirmDialog,
    TchConfirmDialogData,
    TchConfirmDialogResult,
    TchSectionError,
} from '@tch/ui/components';
import { AdminPageShellComponent } from '@tch/ui/console';
import { ErrorViewModel, resolveErrorFeedbackCopy, toErrorViewModel } from '@tch/web/errors';

import {
    NotificationComposerComponent,
    NotificationDraftEvent,
} from '../../components/notification-composer/notification-composer.component';
import { NotificationTableComponent } from '../../components/notification-table/notification-table.component';
import {
    NotificationCategory,
    NotificationItemView,
    NotificationsApi,
    NotificationSeverity,
    NotificationStatus,
} from '../../data-access/notifications-api.service';

const STATUS_OPTIONS: NotificationStatus[] = ['PUBLISHED', 'EXPIRED', 'CANCELLED', 'PURGED'];

@Component({
    selector: 'tch-platform-notifications-page',
    imports: [
        AdminListSurface,
        AdminPageShellComponent,
        NotificationComposerComponent,
        NotificationTableComponent,
        TchSectionError,
        MatButtonModule,
        MatFormFieldModule,
        MatIconModule,
        MatSelectModule,
        MatTooltipModule,
    ],
    templateUrl: './platform-notifications.page.html',
    styleUrl: './platform-notifications.page.scss',
})
export class PlatformNotificationsPage implements OnInit {
    readonly loading = signal(false);
    readonly saving = signal(false);
    readonly error = signal<ErrorViewModel | null>(null);
    readonly composerApiError = signal<ErrorViewModel | null>(null);
    readonly actionError = signal<ErrorViewModel | null>(null);
    readonly actionNotice = signal<{ title: string; message: string } | null>(null);
    readonly notifications = signal<NotificationItemView[]>([]);
    readonly showComposer = signal(false);
    readonly page = signal(0);
    readonly totalElements = signal(0);
    readonly totalPages = signal(1);
    readonly hasNext = signal(false);
    readonly hasPrevious = signal(false);
    readonly unreadCount = computed(() =>
        this.notifications().filter(item => !item.readAt && !item.archivedAt).length,
    );
    readonly searchQuery = signal('');
    readonly statusFilter = signal<NotificationStatus | ''>('');
    readonly severityFilter = signal<NotificationSeverity | ''>('');
    readonly categoryFilter = signal<NotificationCategory | ''>('');
    readonly statusFilterOptions: readonly AdminListStatusOption[] = STATUS_OPTIONS.map(status => ({
        value: status,
        label: {
            PUBLISHED: 'Publiée',
            EXPIRED: 'Expiré',
            CANCELLED: 'Annulée',
            PURGED: 'Purgée',
        }[status],
    }));

    private readonly api = inject(NotificationsApi);
    private readonly dialog = inject(MatDialog);
    private readonly router = inject(Router);
    private readonly translate = inject(TranslateService);

    ngOnInit(): void {
        this.load();
    }

    load(): void {
        this.loading.set(true);
        this.error.set(null);
        this.actionError.set(null);
        this.actionNotice.set(null);
        this.api.listNotifications({
            q: this.searchQuery() || undefined,
            status: this.statusFilter() || undefined,
            severity: this.severityFilter() || undefined,
            category: this.categoryFilter() || undefined,
            page: this.page(),
            size: 20,
        }, { suppressShellFeedback: true }).subscribe({
            next: page => {
                this.notifications.set(page.items ?? []);
                this.totalElements.set(page.totalElements ?? 0);
                this.page.set(page.page);
                this.totalPages.set(page.totalPages || 1);
                this.hasNext.set(page.hasNext ?? false);
                this.hasPrevious.set(page.hasPrevious ?? false);
                this.loading.set(false);
            },
            error: err => {
                this.error.set(this.errorViewModel(err, 'platform.notifications.list'));
                this.loading.set(false);
            },
        });
    }

    applyFilters(): void {
        this.page.set(0);
        this.load();
    }

    resetFilters(): void {
        this.searchQuery.set('');
        this.statusFilter.set('');
        this.severityFilter.set('');
        this.categoryFilter.set('');
        this.page.set(0);
        this.load();
    }

    onSearch(q: string): void {
        this.searchQuery.set(q);
        this.applyFilters();
    }

    onStatusFilter(status: string): void {
        this.statusFilter.set(status as NotificationStatus | '');
        this.applyFilters();
    }

    onSeverityFilter(severity: NotificationSeverity | ''): void {
        this.severityFilter.set(severity);
        this.applyFilters();
    }

    onCategoryFilter(category: NotificationCategory | ''): void {
        this.categoryFilter.set(category);
        this.applyFilters();
    }

    prevPage(): void {
        if (!this.hasPrevious()) return;
        this.page.set(this.page() - 1);
        this.load();
    }

    nextPage(): void {
        if (!this.hasNext()) return;
        this.page.set(this.page() + 1);
        this.load();
    }

    onComposerSubmitted(draft: NotificationDraftEvent): void {
        const request = draft.tenantId
            ? this.api.createTenantNotification(draft.tenantId, draft.payload, { suppressShellFeedback: true })
            : this.api.createNotification(draft.payload, { suppressShellFeedback: true });

        this.saving.set(true);
        this.composerApiError.set(null);
        this.actionError.set(null);
        this.actionNotice.set(null);
        request.subscribe({
            next: () => {
                this.saving.set(false);
                this.showComposer.set(false);
                this.load();
                this.actionNotice.set({
                    title: 'Notification créée',
                    message: draft.payload.titleText,
                });
            },
            error: err => {
                this.saving.set(false);
                this.composerApiError.set(this.errorViewModel(err, 'platform.notifications.create'));
            },
        });
    }

    markRead(item: NotificationItemView): void {
        this.saving.set(true);
        this.actionError.set(null);
        this.actionNotice.set(null);
        this.api.markNotificationRead(this.idOf(item), { suppressShellFeedback: true }).subscribe({
            next: () => {
                this.saving.set(false);
                this.load();
            },
            error: err => {
                this.saving.set(false);
                this.actionError.set(this.errorViewModel(err, 'platform.notifications.markRead'));
            },
        });
    }

    archive(item: NotificationItemView): void {
        this.saving.set(true);
        this.actionError.set(null);
        this.actionNotice.set(null);
        this.api.archiveNotification(this.idOf(item), { suppressShellFeedback: true }).subscribe({
            next: () => {
                this.saving.set(false);
                this.load();
            },
            error: err => {
                this.saving.set(false);
                this.actionError.set(this.errorViewModel(err, 'platform.notifications.archive'));
            },
        });
    }

    publish(item: NotificationItemView): void {
        this.confirmLifecycle({
            title: 'Publier la notification',
            message: `Publier « ${this.titleOf(item)} » maintenant ?`,
            confirmLabel: 'Publier',
            icon: 'publish',
        }, result => {
            this.runLifecycle(
                this.api.publishNotification(this.idOf(item), result.reason ?? 'Publication manuelle', { suppressShellFeedback: true }),
                'Notification publiée.',
            );
        });
    }

    republish(item: NotificationItemView): void {
        this.confirmLifecycle({
            title: 'Republier la notification',
            message: `Créer une nouvelle publication pour « ${this.titleOf(item)} ». Les destinataires la verront comme non lue.`,
            confirmLabel: 'Republier',
            icon: 'campaign',
            sensitive: true,
            requireReason: true,
            auditLabel: 'Republication auditée',
            reasonLabel: 'Raison de la republication',
            confirmCheckboxLabel: 'Je confirme que cette republication est nécessaire et sera tracée.',
        }, result => {
            this.runLifecycle(
                this.api.republishNotification(this.idOf(item), result.reason ?? '', { suppressShellFeedback: true }),
                'Notification republiée.',
            );
        });
    }

    replayRecipients(item: NotificationItemView): void {
        this.confirmLifecycle({
            title: 'Rejouer les destinataires',
            message: `Ajouter les destinataires manquants pour « ${this.titleOf(item)} » sans réinitialiser les lectures existantes ?`,
            confirmLabel: 'Rejouer',
            icon: 'group_add',
        }, () => {
            this.runLifecycle(
                this.api.replayNotificationRecipients(this.idOf(item), { suppressShellFeedback: true }),
                'Destinataires rejoués.',
            );
        });
    }

    cancel(item: NotificationItemView): void {
        this.confirmLifecycle({
            title: 'Annuler la notification',
            message: `Annuler « ${this.titleOf(item)} » pour tous les destinataires ?`,
            confirmLabel: 'Annuler la notification',
            destructive: true,
            icon: 'cancel',
            sensitive: true,
            requireReason: true,
            auditLabel: 'Annulation auditée',
            reasonLabel: 'Raison de l’annulation',
            confirmCheckboxLabel: 'Je confirme que cette annulation est nécessaire et sera tracée.',
        }, result => {
            this.runLifecycle(
                this.api.cancelNotification(this.idOf(item), result.reason ?? '', { suppressShellFeedback: true }),
                'Notification annulée.',
            );
        });
    }

    purgeExpired(dryRun: boolean): void {
        this.confirmLifecycle({
            title: dryRun ? 'Simuler la purge' : 'Purger les notifications expirées',
            message: dryRun
                ? 'Calculer les notifications expirées purgeables sans les modifier ?'
                : 'Marquer les notifications expirées comme purgées ? Cette action est globale.',
            confirmLabel: dryRun ? 'Simuler' : 'Purger',
            destructive: !dryRun,
            icon: dryRun ? 'preview' : 'delete_sweep',
            sensitive: !dryRun,
            requireReason: !dryRun,
            auditLabel: 'Purge auditée',
            reasonLabel: 'Raison de la purge',
            confirmCheckboxLabel: 'Je confirme que cette purge est nécessaire et sera tracée.',
        }, () => {
            this.runLifecycle(
                this.api.purgeExpiredNotifications(dryRun, { suppressShellFeedback: true }),
                dryRun ? 'Purge simulée.' : 'Notifications expirées purgées.',
            );
        });
    }

    openTranslations(item: NotificationItemView): void {
        void this.router.navigate(['/app/platform/catalog/translations'], {
            queryParams: { key: item.titleKey ?? item.messageKey ?? '' },
        });
    }

    private idOf(item: NotificationItemView): string {
        return typeof item.id === 'string' ? item.id : item.id.value;
    }

    private titleOf(item: NotificationItemView): string {
        return item.titleText || item.titleKey || 'Notification';
    }

    private runLifecycle(request: Observable<unknown>, successMessage: string): void {
        this.saving.set(true);
        this.actionError.set(null);
        this.actionNotice.set(null);
        request.subscribe({
            next: () => {
                this.saving.set(false);
                this.load();
                this.actionNotice.set({
                    title: successMessage.replace(/\.$/, ''),
                    message: 'La liste a été actualisée.',
                });
            },
            error: err => {
                this.saving.set(false);
                this.actionError.set(this.errorViewModel(err, 'platform.notifications.lifecycle'));
            },
        });
    }

    private confirmLifecycle(
        data: TchConfirmDialogData,
        confirmed: (result: TchConfirmDialogResult) => void,
    ): void {
        this.dialog.open(TchConfirmDialog, { data })
            .afterClosed()
            .subscribe(result => {
                if (!result?.confirmed) return;
                confirmed(result);
            });
    }

    private errorViewModel(err: unknown, source: string): ErrorViewModel {
        const problem = (err as { error?: ProblemDetail })?.error;
        if (problem) {
            const normalized = webAppErrorFromProblemDetail(problem, source, 'page');
            const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
            return toErrorViewModel(normalized, copy);
        }
        return {
            title: this.translate.instant('common.errors.fallback.title'),
            message: this.translate.instant('common.errors.fallback.message'),
            severity: 'error',
        };
    }
}
