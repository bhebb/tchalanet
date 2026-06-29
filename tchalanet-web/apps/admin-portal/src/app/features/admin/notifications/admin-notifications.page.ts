import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { Observable } from 'rxjs';

import { TchErrorPanel, TchLoading, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '@tch/ui/console';
import {
  AdminNotificationCategory,
  AdminNotificationItem,
  AdminNotificationSeverity,
  AdminNotificationsApi,
} from './admin-notifications-api.service';

const SEVERITIES: AdminNotificationSeverity[] = ['INFO', 'WARNING', 'ERROR', 'CRITICAL'];
const CATEGORIES: AdminNotificationCategory[] = [
  'TENANT_CONFIG',
  'TERMINAL',
  'SALES',
  'DRAW',
  'RESULT',
  'PAYOUT',
  'BATCH',
  'SYSTEM',
  'SECURITY',
];
const CHANNEL_OPTIONS = ['IN_APP', 'EMAIL', 'SMS', 'WHATSAPP'] as const;
type AdminNotificationChannel = (typeof CHANNEL_OPTIONS)[number];

@Component({
  selector: 'tch-admin-notifications-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    AdminCrudShellComponent,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminStatusPillComponent,
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
  ],
  templateUrl: './admin-notifications.page.html',
  styleUrl: './admin-notifications.page.scss',
})
export class AdminNotificationsPage implements OnInit {
  private readonly api = inject(AdminNotificationsApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly severities = SEVERITIES;
  readonly categories = CATEGORIES;
  readonly channelOptions = CHANNEL_OPTIONS;
  readonly displayedColumns = ['createdAt', 'severity', 'title', 'category', 'actions'];

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly pageError = signal<ErrorViewModel | null>(null);
  readonly composerError = signal<ErrorViewModel | null>(null);
  readonly actionError = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<string | null>(null);
  readonly items = signal<AdminNotificationItem[]>([]);
  readonly total = signal(0);
  readonly unreadCount = signal(0);
  readonly page = signal(0);
  readonly totalPages = signal(1);
  readonly hasNext = signal(false);
  readonly hasPrevious = signal(false);
  readonly severityFilter = signal<AdminNotificationSeverity | ''>('');
  readonly categoryFilter = signal<AdminNotificationCategory | ''>('');
  readonly showComposer = signal(false);

  readonly hasUnread = computed(() => this.unreadCount() > 0);

  readonly composerForm = this.fb.nonNullable.group({
    audienceType: ['TENANT_ADMINS' as const],
    severity: ['INFO' as AdminNotificationSeverity],
    category: ['TENANT_CONFIG' as AdminNotificationCategory],
    titleFr: ['', [Validators.required, Validators.maxLength(160)]],
    bodyFr: ['', [Validators.required, Validators.maxLength(1000)]],
    titleEn: ['', [Validators.required, Validators.maxLength(160)]],
    bodyEn: ['', [Validators.required, Validators.maxLength(1000)]],
    titleHt: ['', [Validators.required, Validators.maxLength(160)]],
    bodyHt: ['', [Validators.required, Validators.maxLength(1000)]],
    actionUrl: [''],
    channels: [['IN_APP'] as AdminNotificationChannel[]],
    externalDestination: [''],
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.pageError.set(null);
    this.api
      .list({
        severity: this.severityFilter() || undefined,
        category: this.categoryFilter() || undefined,
        page: this.page(),
        size: 20,
      }, { suppressShellFeedback: true })
      .subscribe({
        next: response => {
          this.items.set(response.items);
          this.total.set(response.totalElements);
          this.page.set(response.page);
          this.totalPages.set(response.totalPages || 1);
          this.hasNext.set(response.hasNext ?? false);
          this.hasPrevious.set(response.hasPrevious ?? false);
          this.loading.set(false);
          this.loadUnreadCount();
        },
        error: err => {
          this.pageError.set(this.errorViewModel(err, 'admin.notifications.list'));
          this.loading.set(false);
        },
      });
  }

  applyFilters(): void {
    this.page.set(0);
    this.load();
  }

  resetFilters(): void {
    this.severityFilter.set('');
    this.categoryFilter.set('');
    this.page.set(0);
    this.load();
  }

  prevPage(): void {
    if (!this.hasPrevious()) return;
    this.page.update(page => page - 1);
    this.load();
  }

  nextPage(): void {
    if (!this.hasNext()) return;
    this.page.update(page => page + 1);
    this.load();
  }

  markRead(item: AdminNotificationItem): void {
    this.runAction(
      this.api.markRead(this.idOf(item), { suppressShellFeedback: true }),
      'Notification marquée comme lue.',
    );
  }

  dismiss(item: AdminNotificationItem): void {
    this.runAction(
      this.api.dismiss(this.idOf(item), { suppressShellFeedback: true }),
      'Notification fermée.',
    );
  }

  markAllRead(): void {
    this.runAction(
      this.api.markAllRead({ suppressShellFeedback: true }),
      'Notifications marquées comme lues.',
    );
  }

  create(): void {
    if (this.composerForm.invalid) {
      this.composerForm.markAllAsTouched();
      return;
    }

    this.composerError.set(null);
    this.actionError.set(null);
    this.actionNotice.set(null);
    const value = this.composerForm.getRawValue();
    const severity = value.severity;
    const externalDestination = value.externalDestination.trim();
    this.saving.set(true);
    this.api
      .create({
        audienceType: value.audienceType,
        severity,
        kind: severity === 'CRITICAL' || severity === 'ERROR' ? 'SYSTEM_ERROR' : 'INFO',
        category: value.category,
        titleText: value.titleFr.trim(),
        messageText: value.bodyFr.trim(),
        translations: {
          fr: { title: value.titleFr.trim(), body: value.bodyFr.trim() },
          en: { title: value.titleEn.trim(), body: value.bodyEn.trim() },
          ht: { title: value.titleHt.trim(), body: value.bodyHt.trim() },
        },
        actionUrl: value.actionUrl.trim() || null,
        payload: externalDestination
          ? {
              to: externalDestination,
              email: externalDestination,
              phone: externalDestination,
              whatsapp: externalDestination,
            }
          : null,
        channels: value.channels,
      }, { suppressShellFeedback: true })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.showComposer.set(false);
          this.composerForm.reset({
            audienceType: 'TENANT_ADMINS',
            severity: 'INFO',
            category: 'TENANT_CONFIG',
            titleFr: '',
            bodyFr: '',
            titleEn: '',
            bodyEn: '',
            titleHt: '',
            bodyHt: '',
            actionUrl: '',
            channels: ['IN_APP'],
            externalDestination: '',
          });
          this.actionNotice.set('Notification créée.');
          this.load();
        },
        error: err => {
          this.saving.set(false);
          this.composerError.set(this.errorViewModel(err, 'admin.notifications.create'));
        },
      });
  }

  idOf(item: AdminNotificationItem): string {
    return typeof item.id === 'string' ? item.id : item.id.value;
  }

  titleOf(item: AdminNotificationItem): string {
    return item.titleText || item.titleKey || 'Notification';
  }

  messageOf(item: AdminNotificationItem): string {
    return item.messageText || item.messageKey || '';
  }

  severityTone(severity: AdminNotificationSeverity): AdminStatusTone {
    switch (severity) {
      case 'CRITICAL':
      case 'ERROR':
        return 'danger';
      case 'WARNING':
        return 'warning';
      default:
        return 'neutral';
    }
  }

  private loadUnreadCount(): void {
    this.api.unreadCount({ suppressShellFeedback: true }).subscribe({
      next: value => this.unreadCount.set(value.unreadCount ?? 0),
      error: () => this.unreadCount.set(0),
    });
  }

  private runAction(action: Observable<boolean>, successMessage: string): void {
    this.actionError.set(null);
    this.actionNotice.set(null);
    this.saving.set(true);
    action.subscribe({
      next: () => {
        this.saving.set(false);
        this.actionNotice.set(successMessage);
        this.load();
      },
      error: err => {
        this.saving.set(false);
        this.actionError.set(this.errorViewModel(err, 'admin.notifications.action'));
      },
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
