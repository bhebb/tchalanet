import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminCrudShellComponent } from '../../shared/admin-ui/admin-crud-shell.component';
import { AdminEmptyStateComponent } from '../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../shared/admin-ui/admin-page-shell.component';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '../../shared/admin-ui/admin-status-pill.component';
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
  private readonly snackBar = inject(MatSnackBar);

  readonly severities = SEVERITIES;
  readonly categories = CATEGORIES;
  readonly channelOptions = CHANNEL_OPTIONS;
  readonly displayedColumns = ['createdAt', 'severity', 'title', 'category', 'actions'];

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly items = signal<AdminNotificationItem[]>([]);
  readonly total = signal(0);
  readonly unreadCount = signal(0);
  readonly page = signal(0);
  readonly totalPages = signal(1);
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
    this.error.set(null);
    this.api
      .list({
        severity: this.severityFilter() || undefined,
        category: this.categoryFilter() || undefined,
        page: this.page(),
        size: 20,
      })
      .subscribe({
        next: response => {
          this.items.set(response.items ?? response.content ?? []);
          this.total.set(response.totalElements ?? 0);
          this.totalPages.set(response.totalPages || 1);
          this.loading.set(false);
          this.loadUnreadCount();
        },
        error: err => {
          this.error.set(this.errorMessage(err));
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
    if (this.page() === 0) return;
    this.page.update(page => page - 1);
    this.load();
  }

  nextPage(): void {
    if (this.page() + 1 >= this.totalPages()) return;
    this.page.update(page => page + 1);
    this.load();
  }

  markRead(item: AdminNotificationItem): void {
    this.runAction(this.api.markRead(this.idOf(item)), 'Notification marquée comme lue.');
  }

  dismiss(item: AdminNotificationItem): void {
    this.runAction(this.api.dismiss(this.idOf(item)), 'Notification fermée.');
  }

  markAllRead(): void {
    this.runAction(this.api.markAllRead(), 'Notifications marquées comme lues.');
  }

  create(): void {
    if (this.composerForm.invalid) {
      this.composerForm.markAllAsTouched();
      return;
    }

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
      })
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
          this.snackBar.open('Notification créée.', 'OK', { duration: 2500 });
          this.load();
        },
        error: err => {
          this.saving.set(false);
          this.snackBar.open(this.errorMessage(err), 'OK', { duration: 5000 });
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
    this.api.unreadCount().subscribe({
      next: value => this.unreadCount.set(value.unreadCount ?? 0),
      error: () => this.unreadCount.set(0),
    });
  }

  private runAction(action: ReturnType<AdminNotificationsApi['markRead']>, successMessage: string): void {
    this.saving.set(true);
    action.subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open(successMessage, 'OK', { duration: 2500 });
        this.load();
      },
      error: err => {
        this.saving.set(false);
        this.snackBar.open(this.errorMessage(err), 'OK', { duration: 5000 });
      },
    });
  }

  private errorMessage(err: unknown): string {
    const problem = (err as { error?: { title?: string; detail?: string; message?: string } })?.error;
    return problem?.detail ?? problem?.title ?? problem?.message ?? 'Erreur de chargement.';
  }
}
