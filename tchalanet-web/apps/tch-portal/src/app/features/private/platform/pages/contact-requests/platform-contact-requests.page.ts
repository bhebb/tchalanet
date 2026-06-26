import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

import { AdminListStatusOption, AdminListSurface, TchErrorPanel, TchLoading } from '@tch/ui/components';
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
  private readonly snackBar = inject(MatSnackBar);

  readonly statusOptions = STATUS_OPTIONS;
  readonly intentOptions = INTENT_OPTIONS;
  readonly displayedColumns = ['createdAt', 'reference', 'contact', 'intent', 'status', 'location', 'actions'];

  readonly searchQuery = signal('');
  readonly statusFilter = signal<ContactRequestStatus | ''>('');
  readonly intentFilter = signal<ContactRequestIntent | ''>('');

  readonly loading = signal(false);
  readonly detailLoading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
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

    this.api
      .listContactRequests({
        q: this.searchQuery() || undefined,
        status: this.statusFilter() || undefined,
        intent: this.intentFilter() || undefined,
        page: this.page(),
        size: 20,
      })
      .subscribe({
        next: page => {
          this.requests.set(page.items ?? []);
          this.totalElements.set(page.totalElements ?? 0);
          this.totalPages.set(page.totalPages || 1);
          this.loading.set(false);
        },
        error: err => {
          this.error.set(this.errorMessage(err));
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
    this.api.getContactRequest(row.id).subscribe({
      next: detail => {
        this.selected.set(detail);
        this.detailLoading.set(false);
      },
      error: err => {
        this.detailLoading.set(false);
        this.snackBar.open(this.errorMessage(err), 'OK', { duration: 5000 });
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
    this.api.updateContactStatus(selected.id, status).subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open('Statut mis à jour.', 'OK', { duration: 3000 });
        this.open({ ...selected, status });
        this.load();
      },
      error: err => {
        this.saving.set(false);
        this.snackBar.open(this.errorMessage(err), 'OK', { duration: 5000 });
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
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.snackBar.open('Notes mises à jour.', 'OK', { duration: 3000 });
          this.open(selected);
        },
        error: err => {
          this.saving.set(false);
          this.snackBar.open(this.errorMessage(err), 'OK', { duration: 5000 });
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

  private errorMessage(err: unknown): string {
    return (err as { error?: { title?: string; detail?: string } })?.error?.title
      ?? (err as { error?: { detail?: string } })?.error?.detail
      ?? 'Erreur de chargement.';
  }
}
