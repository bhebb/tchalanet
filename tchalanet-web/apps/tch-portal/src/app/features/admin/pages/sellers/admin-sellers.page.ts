import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import {
  TchLoading,
  TchErrorPanel,
  AdminPageHeader,
  AdminListToolbar,
  AdminDataTable,
  AdminMobileCardList,
  AdminStatusBadge,
  AdminEmptyState,
  type AdminFilter,
} from '@tch/ui/components';
import { catchError, map, of, startWith, switchMap } from 'rxjs';

import {
  SellerTerminalAdminApi,
  SellerTerminalRow,
  SellerTerminalStatus,
} from '../../seller-terminal-admin.api.service';

const PAGE_SIZE = 20;

type ListState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | {
      readonly status: 'ready';
      readonly items: readonly SellerTerminalRow[];
      readonly totalElements: number;
      readonly totalPages: number;
    };

type StatusFilter = SellerTerminalStatus | 'ALL';

const FILTERS: readonly AdminFilter[] = [
  { code: 'ALL',      label: 'Tous' },
  { code: 'ACTIVE',   label: 'Actifs' },
  { code: 'PENDING',  label: 'En attente' },
  { code: 'BLOCKED',  label: 'Bloqués' },
  { code: 'DISABLED', label: 'Désactivés' },
];

const DISPLAYED_COLUMNS = ['name', 'phone', 'commission', 'status', 'actions'] as const;

@Component({
  selector: 'tch-admin-sellers-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    TchLoading,
    TchErrorPanel,
    AdminPageHeader,
    AdminListToolbar,
    AdminDataTable,
    AdminMobileCardList,
    AdminStatusBadge,
    AdminEmptyState,
  ],
  templateUrl: './admin-sellers.page.html',
  styleUrl: './admin-sellers.page.scss',
})
export class AdminSellersPage {
  protected readonly router = inject(Router);
  private readonly api = inject(SellerTerminalAdminApi);


  readonly filters = FILTERS;
  readonly displayedColumns = DISPLAYED_COLUMNS;

  readonly query = signal('');
  readonly statusFilter = signal<StatusFilter>('ALL');
  readonly page = signal(0);

  private readonly params = computed(() => ({
    q: this.query(),
    status: this.statusFilter(),
    p: this.page(),
  }));

  readonly state = toSignal(
    toObservable(this.params).pipe(
      switchMap(({ q, status, p }) =>
        this.api
          .list({
            q: q || undefined,
            status: status === 'ALL' ? undefined : status,
            page: p,
            size: PAGE_SIZE,
          })
          .pipe(
            map(
              page =>
                ({
                  status: 'ready',
                  items: page.items,
                  totalElements: page.totalElements,
                  totalPages: page.totalPages,
                }) as ListState,
            ),
            catchError(() => of({ status: 'error' } as ListState)),
            startWith({ status: 'loading' } as ListState),
          ),
      ),
    ),
    { initialValue: { status: 'loading' } as ListState },
  );

  statusLabel(status: SellerTerminalStatus): string {
    const LABELS: Record<SellerTerminalStatus, string> = {
      ACTIVE: 'Actif',
      PENDING: 'En attente',
      BLOCKED: 'Bloqué',
      DISABLED: 'Désactivé',
    };
    return LABELS[status] ?? status;
  }

  onSearchChange(value: string): void {
    this.query.set(value);
    this.page.set(0);
  }

  onFilterChange(code: string): void {
    this.statusFilter.set(code as StatusFilter);
    this.page.set(0);
  }

  prevPage(): void {
    this.page.update(p => Math.max(0, p - 1));
  }

  nextPage(): void {
    this.page.update(p => p + 1);
  }
}
