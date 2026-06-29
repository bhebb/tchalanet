import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateService } from '@ngx-translate/core';
import { Observable, map } from 'rxjs';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import {
  AdminListStatusOption,
  AdminListSurface,
  TchErrorPanel,
  TchLoading,
  TchSearchOption,
  TchSearchSelect,
  TchSectionError,
} from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminEmptyStateComponent } from '../../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '../../../../shared/admin-ui/admin-status-pill.component';
import {
  CommunicationChannel,
  CommunicationMessageView,
  CommunicationQueueSummary,
  DeliveryStatus,
  PlatformCommunicationApi,
} from '../../data-access/platform-communication-api.service';
import {
  PlatformTenantsApi,
  TenantSummaryView,
} from '../../../tenants/data-access/platform-tenants-api.service';

@Component({
  selector: 'tch-platform-communication-outbox-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    AdminListSurface,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminStatusPillComponent,
    TchErrorPanel,
    TchLoading,
    TchSearchSelect,
    TchSectionError,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './platform-communication-outbox.page.html',
  styleUrls: ['./platform-communication.page.scss'],
})
export class PlatformCommunicationOutboxPage implements OnInit {
  private readonly api = inject(PlatformCommunicationApi);
  private readonly tenantsApi = inject(PlatformTenantsApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly statuses: DeliveryStatus[] = ['PENDING', 'DISPATCHING', 'SENT', 'FAILED', 'SKIPPED', 'CANCELLED'];
  readonly channels: CommunicationChannel[] = [
    'SLACK_INTERNAL',
    'SLACK',
    'EMAIL',
    'SMS',
    'WHATSAPP',
    'SLACK_TENANT_WEBHOOK',
    'PUSH',
  ];
  readonly statusFilterOptions: readonly AdminListStatusOption[] = this.statuses.map(status => ({
    value: status,
    label: status,
  }));
  readonly channelFilterOptions: readonly AdminListStatusOption[] = this.channels.map(channel => ({
    value: channel,
    label: channel,
  }));
  readonly displayedColumns = ['createdAt', 'status', 'channel', 'recipient', 'template', 'nextAttempt', 'expand'];

  readonly filterForm = this.fb.nonNullable.group({
    status: ['' as DeliveryStatus | ''],
    channel: ['' as CommunicationChannel | ''],
    tenantId: [''],
    recipient: [''],
  });

  readonly loading = signal(false);
  readonly dispatching = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly actionFeedback = signal<ErrorViewModel | null>(null);
  readonly summary = signal<CommunicationQueueSummary | null>(null);
  readonly messages = signal<CommunicationMessageView[]>([]);
  readonly expandedId = signal<string | null>(null);
  readonly page = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);

  readonly searchTenants = (query: string): Observable<readonly TchSearchOption<TenantSummaryView>[]> =>
    this.tenantsApi.listTenants({ q: query, page: 0, size: 12, status: null }, { suppressShellFeedback: true }).pipe(
      map(page => page.items.map(tenant => this.toTenantOption(tenant))),
    );

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    const v = this.filterForm.getRawValue();
    this.api.listMessages(
      {
        status: v.status,
        channel: v.channel,
        tenantId: v.tenantId,
        recipient: v.recipient,
        page: this.page(),
        size: 20,
      },
      { suppressShellFeedback: true },
    ).subscribe({
      next: view => {
        this.summary.set(view.summary);
        this.messages.set(view.messages.items);
        this.totalElements.set(view.messages.totalElements);
        this.totalPages.set(view.messages.totalPages || 1);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(this.errorViewModel(err, 'platform.communication.outbox.list'));
        this.loading.set(false);
      },
    });
  }

  applyFilters(): void {
    this.page.set(0);
    this.load();
  }

  onSearch(query: string): void {
    this.filterForm.patchValue({ recipient: query });
    this.applyFilters();
  }

  onStatusFilter(status: string): void {
    this.filterForm.patchValue({ status: status as DeliveryStatus | '' });
    this.applyFilters();
  }

  onChannelFilter(channel: string): void {
    this.filterForm.patchValue({ channel: channel as CommunicationChannel | '' });
    this.applyFilters();
  }

  resetFilters(): void {
    this.filterForm.reset();
    this.page.set(0);
    this.load();
  }

  hasActiveFilters(): boolean {
    const value = this.filterForm.getRawValue();
    return Boolean(value.status || value.channel || value.tenantId || value.recipient);
  }

  prevPage(): void {
    this.page.set(this.page() - 1);
    this.load();
  }

  nextPage(): void {
    this.page.set(this.page() + 1);
    this.load();
  }

  toggleExpand(id: string): void {
    this.expandedId.set(this.expandedId() === id ? null : id);
  }

  dispatchDue(): void {
    this.dispatching.set(true);
    this.actionFeedback.set(null);
    this.api.dispatchDue({ suppressShellFeedback: true }).subscribe({
      next: result => {
        this.dispatching.set(false);
        this.actionFeedback.set({
          title: 'Dispatch terminé',
          message: `${result.dispatched} message(s) dispatché(s).`,
          severity: 'info',
        });
        this.load();
      },
      error: (err: unknown) => {
        this.dispatching.set(false);
        this.actionFeedback.set(this.errorViewModel(err, 'platform.communication.outbox.dispatch'));
      },
    });
  }

  selectTenantFilter(option: TchSearchOption | null): void {
    const tenant = option?.data as TenantSummaryView | undefined;
    this.filterForm.patchValue({ tenantId: tenant?.id ?? tenant?.tenantId ?? '' });
    this.applyFilters();
  }

  statusTone(status: DeliveryStatus): AdminStatusTone {
    if (status === 'SENT') return 'success';
    if (status === 'FAILED' || status === 'CANCELLED') return 'danger';
    if (status === 'PENDING' || status === 'DISPATCHING') return 'warning';
    return 'neutral';
  }

  channelTone(channel: CommunicationChannel): AdminStatusTone {
    if (channel.includes('SLACK')) return 'info';
    if (channel === 'EMAIL') return 'success';
    if (channel === 'SMS' || channel === 'WHATSAPP') return 'warning';
    return 'neutral';
  }

  private toTenantOption(tenant: TenantSummaryView): TchSearchOption<TenantSummaryView> {
    return {
      id: tenant.id ?? tenant.tenantId ?? tenant.code,
      title: tenant.name,
      subtitle: tenant.code,
      badge: tenant.status,
      icon: 'apartment',
      data: tenant,
    };
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
