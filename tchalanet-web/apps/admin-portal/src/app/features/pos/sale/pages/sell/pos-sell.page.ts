import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Sort, SortDirection } from '@angular/material/sort';
import { TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';

import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { ErrorViewModel, resolveErrorFeedbackCopy, toErrorViewModel } from '@tch/web/errors';
import { AdminListStatusOption, AdminListSurface, TchErrorPanel, TchLoading, TchNotice } from '@tch/ui/components';

import { PosSaleApiService } from '../../data-access/pos-sale-api.service';
import { PosSellerTerminalPickerView } from '../../data-access/pos-sale.models';
import { PosSellerTerminalListComponent } from '../../components/pos-seller-terminal-list/pos-seller-terminal-list.component';

const PAGE_SIZE = 20;

@Component({
  selector: 'tch-pos-sell-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    MatIconModule,
    AdminListSurface,
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    TchErrorPanel,
    TchLoading,
    TchNotice,
    PosSellerTerminalListComponent,
  ],
  templateUrl: './pos-sell.page.html',
  styleUrls: ['./pos-sell.page.scss'],
})
export class PosSellPage implements OnInit {
  private readonly api = inject(PosSaleApiService);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly sellerTerminals = signal<PosSellerTerminalPickerView[]>([]);
  readonly searchQuery = signal('');
  readonly statusFilter = signal('');
  readonly sort = signal('displayName,asc');
  readonly page = signal(0);
  readonly total = signal(0);
  readonly totalPages = signal(1);
  readonly hasNext = signal(false);
  readonly hasPrevious = signal(false);

  readonly statusOptions: readonly AdminListStatusOption[] = [
    { value: 'ACTIVE', label: 'Actifs' },
    { value: 'BLOCKED', label: 'Bloqués' },
    { value: 'INACTIVE', label: 'Inactifs' },
    { value: 'DISABLED', label: 'Désactivés' },
    { value: 'PENDING', label: 'En attente' },
  ];

  ngOnInit(): void {
    this.loadSellerTerminals();
  }

  loadSellerTerminals(): void {
    this.loading.set(true);
    this.error.set(null);

    this.api.listSellerTerminalsForSale(
      {
        q: this.searchQuery(),
        status: this.statusFilter(),
        sort: this.sort(),
        page: this.page(),
        size: PAGE_SIZE,
      },
      { suppressShellFeedback: true },
    ).subscribe({
      next: page => {
        this.sellerTerminals.set(page.items);
        this.total.set(page.totalElements);
        this.totalPages.set(page.totalPages || 1);
        this.page.set(page.page);
        this.hasNext.set(page.hasNext ?? false);
        this.hasPrevious.set(page.hasPrevious ?? false);
        this.loading.set(false);
      },
      error: err => {
        this.error.set(this.errorViewModel(err));
        this.sellerTerminals.set([]);
        this.total.set(0);
        this.totalPages.set(1);
        this.hasNext.set(false);
        this.hasPrevious.set(false);
        this.loading.set(false);
      },
    });
  }

  openSale(sellerTerminal: PosSellerTerminalPickerView): void {
    if (!this.canSellAs(sellerTerminal)) return;
    void this.router.navigate(['/app/admin/pos/sale', sellerTerminal.sellerTerminalId]);
  }

  canSellAs(sellerTerminal: PosSellerTerminalPickerView): boolean {
    return sellerTerminal.status === 'ACTIVE';
  }

  onSearchFilter(query: string): void {
    this.searchQuery.set(query);
    this.page.set(0);
    this.loadSellerTerminals();
  }

  onStatusFilter(status: string): void {
    this.statusFilter.set(status);
    this.page.set(0);
    this.loadSellerTerminals();
  }

  resetFilters(): void {
    this.searchQuery.set('');
    this.statusFilter.set('');
    this.page.set(0);
    this.loadSellerTerminals();
  }

  onSortChange(sort: Sort): void {
    const direction = sort.direction || 'asc';
    this.sort.set(`${sort.active},${direction}`);
    this.page.set(0);
    this.loadSellerTerminals();
  }

  prevPage(): void {
    if (!this.hasPrevious()) return;
    this.page.update(value => Math.max(0, value - 1));
    this.loadSellerTerminals();
  }

  nextPage(): void {
    if (!this.hasNext()) return;
    this.page.update(value => value + 1);
    this.loadSellerTerminals();
  }

  sortActive(): string {
    return this.sort().split(',')[0] || 'displayName';
  }

  sortDirection(): SortDirection {
    const direction = this.sort().split(',')[1];
    return direction === 'desc' ? 'desc' : 'asc';
  }

  private errorViewModel(err: unknown): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, 'admin.pos.sale.sellerPicker', 'page');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return toErrorViewModel(normalized, copy);
    }

    return {
      title: 'Impossible de charger les vendeurs.',
      message: 'Un probleme est survenu. Reessayez dans quelques instants.',
      severity: 'error',
    };
  }
}
