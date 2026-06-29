import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { AdminListStatusOption, AdminListSurface, TchErrorPanel, TchLoading, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '../../../../../core/api/error-feedback-copy';
import { ErrorViewModel, toErrorViewModel } from '../../../../../core/api/local-error-routing';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '../../../shared/admin-ui/admin-status-pill.component';
import {
  ContactRequestAdminDetailView,
  ContactRequestIntent,
  ContactRequestStatus,
  ContactRequestSummaryView,
  PlatformSupportApi,
} from '../../support/data-access/platform-support-api.service';

const STATUS_OPTIONS: ContactRequestStatus[] = ['RECEIVED', 'CONTACTED', 'QUALIFIED', 'CLOSED', 'SPAM'];
const INTENT_OPTIONS: ContactRequestIntent[] = [
  'REQUEST_DEMO',
  'BECOME_OPERATOR',
  'SUPPORT',
  'PARTNERSHIP',
  'OTHER',
];

@Component({
  selector: 'tch-platform-contact-requests-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    AdminListSurface,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminStatusPillComponent,
    TchErrorPanel,
    TchLoading,
    TchSectionError,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './platform-contact-requests.page.html',
  styleUrl: './platform-contact-requests.page.scss',
})
export class PlatformContactRequestsPage implements OnInit {
  private readonly api = inject(PlatformSupportApi);
  private readonly translate = inject(TranslateService);

  readonly statusOptions = STATUS_OPTIONS;
  readonly intentOptions = INTENT_OPTIONS;
  readonly displayedColumns = ['createdAt', 'reference', 'contact', 'intent', 'status', 'location', 'actions'];

  readonly searchQuery = signal('');
  readonly statusFilter = signal<ContactRequestStatus | ''>('');
  readonly intentFilter = signal<ContactRequestIntent | ''>('');

  readonly loading = signal(false);
  readonly detailLoading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly detailError = signal<ErrorViewModel | null>(null);
  readonly actionError = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<{ title: string; message: string } | null>(null);
  readonly requests = signal<ContactRequestSummaryView[]>([]);
  readonly selected = signal<ContactRequestAdminDetailView | null>(null);
  readonly page = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);

  readonly hasSelection = computed(() => this.selected() !== null);
  readonly statusFilterOptions: readonly AdminListStatusOption[] = STATUS_OPTIONS.map(status => ({
    value: status,
    label: {
      RECEIVED: 'Reçu',
      CONTACTED: 'Contacté',
      QUALIFIED: 'Qualifié',
      CLOSED: 'Fermé',
      SPAM: 'Spam',
    }[status],
  }));

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.actionError.set(null);
    this.actionNotice.set(null);

    this.api
      .listContactRequests({
        q: this.searchQuery() || undefined,
        status: this.statusFilter() || undefined,
        intent: this.intentFilter() || undefined,
        page: this.page(),
        size: 20,
      }, { suppressShellFeedback: true })
      .subscribe({
        next: page => {
          this.requests.set(page.items ?? []);
          this.totalElements.set(page.totalElements ?? 0);
          this.totalPages.set(page.totalPages || 1);
          this.loading.set(false);
        },
        error: err => {
          this.error.set(this.errorViewModel(err, 'platform.contactRequests.list'));
          this.loading.set(false);
        },
      });
  }

  applyFilters(): void {
    this.page.set(0);
    this.selected.set(null);
    this.load();
  }

  resetFilters(): void {
    this.searchQuery.set('');
    this.statusFilter.set('');
    this.intentFilter.set('');
    this.page.set(0);
    this.selected.set(null);
    this.load();
  }

  onSearch(q: string): void {
    this.searchQuery.set(q);
    this.applyFilters();
  }

  onStatusFilter(status: string): void {
    this.statusFilter.set(status as ContactRequestStatus | '');
    this.applyFilters();
  }

  onIntentFilter(intent: ContactRequestIntent | ''): void {
    this.intentFilter.set(intent);
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

  open(row: ContactRequestSummaryView): void {
    this.detailLoading.set(true);
    this.detailError.set(null);
    this.actionError.set(null);
    this.actionNotice.set(null);
    this.api.getContactRequest(row.id, { suppressShellFeedback: true }).subscribe({
      next: detail => {
        this.selected.set(detail);
        this.detailLoading.set(false);
      },
      error: err => {
        this.detailLoading.set(false);
        this.detailError.set(this.errorViewModel(err, 'platform.contactRequests.detail'));
      },
    });
  }

  closeDetail(): void {
    this.selected.set(null);
  }

  updateStatus(status: ContactRequestStatus): void {
    const selected = this.selected();
    if (!selected || selected.status === status) return;
    this.saving.set(true);
    this.actionError.set(null);
    this.actionNotice.set(null);
    this.api.updateContactStatus(selected.id, status, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.saving.set(false);
        this.open({ ...selected, status });
        this.load();
        this.actionNotice.set({
          title: 'Statut mis à jour',
          message: this.statusLabel(status),
        });
      },
      error: err => {
        this.saving.set(false);
        this.actionError.set(this.errorViewModel(err, 'platform.contactRequests.status'));
      },
    });
  }

  updateNotes(): void {
    const selected = this.selected();
    if (!selected) return;
    const notes = prompt('Notes internes', selected.internalNotes ?? '');
    if (notes === null) return;

    this.saving.set(true);
    this.api
      .updateContactNotes(selected.id, {
        internalNotes: notes.trim() || null,
        externalTool: selected.externalTool,
        externalReference: selected.externalReference,
      }, { suppressShellFeedback: true })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.open(selected);
          this.actionNotice.set({
            title: 'Notes mises à jour',
            message: selected.reference,
          });
        },
        error: err => {
          this.saving.set(false);
          this.actionError.set(this.errorViewModel(err, 'platform.contactRequests.notes'));
        },
      });
  }

  statusTone(status: ContactRequestStatus): AdminStatusTone {
    if (status === 'RECEIVED') return 'warning';
    if (status === 'QUALIFIED' || status === 'CLOSED') return 'success';
    if (status === 'SPAM') return 'neutral';
    return 'info';
  }

  intentLabel(intent: ContactRequestIntent): string {
    return {
      REQUEST_DEMO: 'Demande de démo',
      BECOME_OPERATOR: 'Devenir opérateur',
      SUPPORT: 'Support',
      PARTNERSHIP: 'Partenariat',
      OTHER: 'Autre',
    }[intent];
  }

  statusLabel(status: ContactRequestStatus): string {
    return {
      RECEIVED: 'Reçu',
      CONTACTED: 'Contacté',
      QUALIFIED: 'Qualifié',
      CLOSED: 'Fermé',
      SPAM: 'Spam',
    }[status];
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
