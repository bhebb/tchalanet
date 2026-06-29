import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
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
  TchErrorPanel,
  TchLoading,
  TchSectionError,
} from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '@tch/ui/console';
import {
  PlatformRecipientPickerComponent,
  PlatformRecipientPickerSelection,
} from '../../shared/recipient-picker/platform-recipient-picker.component';
import {
  NotificationCategory,
  NotificationAudienceType,
  NotificationItemView,
  NotificationSeverity,
  NotificationStatus,
  NotificationChannel,
  PlatformSupportApi,
} from '../../support/data-access/platform-support-api.service';

const STATUS_OPTIONS: NotificationStatus[] = ['PUBLISHED', 'EXPIRED', 'CANCELLED', 'PURGED'];
const SEVERITY_OPTIONS: NotificationSeverity[] = ['INFO', 'WARNING', 'ERROR', 'CRITICAL'];
const CATEGORY_OPTIONS: NotificationCategory[] = ['SYSTEM', 'SECURITY', 'BATCH', 'TENANT_CONFIG', 'DRAW', 'RESULT'];
const AUDIENCE_OPTIONS: NotificationAudienceType[] = ['PLATFORM_ADMINS', 'ALL_APP_USERS'];
const CHANNEL_OPTIONS: NotificationChannel[] = ['IN_APP', 'SLACK', 'EMAIL', 'SMS', 'WHATSAPP'];
type TargetMode = 'BROADCAST' | 'SPECIFIC';

@Component({
  selector: 'tch-platform-notifications-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    AdminListSurface,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminStatusPillComponent,
    PlatformRecipientPickerComponent,
    TchErrorPanel,
    TchLoading,
    TchSectionError,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTabsModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './platform-notifications.page.html',
  styleUrl: './platform-notifications.page.scss',
})
export class PlatformNotificationsPage implements OnInit {
  private readonly api = inject(PlatformSupportApi);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);

  readonly statusOptions = STATUS_OPTIONS;
  readonly severityOptions = SEVERITY_OPTIONS;
  readonly categoryOptions = CATEGORY_OPTIONS;
  readonly audienceOptions = AUDIENCE_OPTIONS;
  readonly channelOptions = CHANNEL_OPTIONS;
  readonly displayedColumns = ['createdAt', 'severity', 'title', 'category', 'status', 'actions'];

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly composerError = signal<ErrorViewModel | null>(null);
  readonly actionError = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<{ title: string; message: string } | null>(null);
  readonly notifications = signal<NotificationItemView[]>([]);
  readonly showComposer = signal(false);
  readonly recipientSelection = signal<PlatformRecipientPickerSelection | null>(null);
  readonly page = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);

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

  readonly composerForm = this.fb.nonNullable.group({
    titleFr: ['', [Validators.required, Validators.maxLength(160)]],
    bodyFr: ['', [Validators.required, Validators.maxLength(1000)]],
    titleEn: ['', [Validators.required, Validators.maxLength(160)]],
    bodyEn: ['', [Validators.required, Validators.maxLength(1000)]],
    titleHt: ['', [Validators.required, Validators.maxLength(160)]],
    bodyHt: ['', [Validators.required, Validators.maxLength(1000)]],
    severity: ['WARNING' as NotificationSeverity],
    category: ['SYSTEM' as NotificationCategory],
    targetMode: ['BROADCAST' as TargetMode],
    audienceType: ['PLATFORM_ADMINS' as NotificationAudienceType],
    actionUrl: [''],
    channels: [['IN_APP'] as NotificationChannel[]],
    externalDestination: [''],
  });

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
        this.totalPages.set(page.totalPages || 1);
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
    if (this.page() === 0) return;
    this.page.set(this.page() - 1);
    this.load();
  }

  nextPage(): void {
    if (this.page() + 1 >= this.totalPages()) return;
    this.page.set(this.page() + 1);
    this.load();
  }

  create(): void {
    if (this.composerForm.invalid) {
      this.composerForm.markAllAsTouched();
      return;
    }

    const value = this.composerForm.getRawValue();
    const externalDestination = value.externalDestination.trim();
    const recipientSelection = this.recipientSelection();
    const specificTargets = value.targetMode === 'SPECIFIC' ? recipientSelection?.targets ?? [] : [];
    if (value.targetMode === 'SPECIFIC' && specificTargets.length === 0) {
      this.composerError.set({
        title: 'Destinataire requis',
        message: 'Sélectionnez au moins un destinataire avant d’envoyer la notification.',
        severity: 'error',
      });
      return;
    }

    const payload = {
      sourceType: 'platform-admin',
      sourceId: null,
      dedupeKey: null,
      audienceType: value.targetMode === 'SPECIFIC' ? 'SPECIFIC_ACTORS' as NotificationAudienceType : value.audienceType,
      targets: value.targetMode === 'SPECIFIC' ? specificTargets : undefined,
      severity: value.severity,
      kind: value.severity === 'CRITICAL' || value.severity === 'ERROR' ? 'SYSTEM_ERROR' as const : 'WARNING' as const,
      category: value.category,
      titleText: value.titleFr.trim(),
      messageText: value.bodyFr.trim(),
      translations: {
        fr: { title: value.titleFr.trim(), body: value.bodyFr.trim() },
        en: { title: value.titleEn.trim(), body: value.bodyEn.trim() },
        ht: { title: value.titleHt.trim(), body: value.bodyHt.trim() },
      },
      actionType: value.actionUrl.trim() ? 'LINK' : null,
      actionUrl: value.actionUrl.trim() || null,
      payload: externalDestination
        ? {
            to: externalDestination,
            email: externalDestination,
            phone: externalDestination,
            whatsapp: externalDestination,
          }
        : null,
      expiresAt: null,
      channels: value.channels,
    };

    const request = value.targetMode === 'SPECIFIC' && recipientSelection?.tenantId
      ? this.api.createTenantNotification(
          recipientSelection.tenantId,
          payload,
          { suppressShellFeedback: true },
        )
      : this.api.createNotification(payload, { suppressShellFeedback: true });

    this.saving.set(true);
    this.composerError.set(null);
    this.actionError.set(null);
    this.actionNotice.set(null);
    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.showComposer.set(false);
        this.recipientSelection.set(null);
        this.composerForm.reset({
          titleFr: '',
          bodyFr: '',
          titleEn: '',
          bodyEn: '',
          titleHt: '',
          bodyHt: '',
          severity: 'WARNING',
          category: 'SYSTEM',
          targetMode: 'BROADCAST',
          audienceType: 'PLATFORM_ADMINS',
          actionUrl: '',
          channels: ['IN_APP'],
          externalDestination: '',
        });
        this.load();
        this.actionNotice.set({
          title: 'Notification créée',
          message: value.titleFr.trim(),
        });
      },
      error: err => {
        this.saving.set(false);
        this.composerError.set(this.errorViewModel(err, 'platform.notifications.create'));
      },
    });
  }

  updateRecipients(selection: PlatformRecipientPickerSelection): void {
    this.recipientSelection.set(selection);
    this.composerError.set(null);
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
        this.api.publishNotification(
          this.idOf(item),
          result.reason ?? 'Publication manuelle',
          { suppressShellFeedback: true },
        ),
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
        this.api.republishNotification(
          this.idOf(item),
          result.reason ?? '',
          { suppressShellFeedback: true },
        ),
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
        this.api.cancelNotification(
          this.idOf(item),
          result.reason ?? '',
          { suppressShellFeedback: true },
        ),
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

  openTranslations(item: NotificationItemView): void {
    void this.router.navigate(['/app/platform/catalog/translations'], {
      queryParams: { key: item.titleKey ?? item.messageKey ?? '' },
    });
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
    return {
      PUBLISHED: 'Publiée',
      EXPIRED: 'Expiré',
      CANCELLED: 'Annulée',
      PURGED: 'Purgée',
    }[status];
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
