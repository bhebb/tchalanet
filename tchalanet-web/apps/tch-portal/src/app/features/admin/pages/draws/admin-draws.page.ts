import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
  computed,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe, SlicePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, of, startWith, switchMap } from 'rxjs';
import {
  TchLoading,
  TchErrorPanel,
  AdminPageHeader,
  AdminEmptyState,
} from '@tch/ui/components';

import { DrawAdminApi, DrawStatus, DrawSummary } from '../../draw-admin.api.service';

type Filter = 'all' | 'upcoming' | 'past';

interface FilterOption {
  readonly value: Filter;
  readonly label: string;
}

const PAGE_SIZE = 30;

type PageState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | {
      readonly status: 'ready';
      readonly items: readonly DrawSummary[];
      readonly totalPages: number;
    };

const STATUS_LABELS: Record<DrawStatus, string> = {
  SCHEDULED: 'Planifié',
  OPEN: 'Ouvert',
  CLOSED: 'Fermé',
  RESULTED: 'Résulté',
  SETTLED: 'Réglé',
  CANCELED: 'Annulé',
  ARCHIVED: 'Archivé',
};

@Component({
  selector: 'tch-admin-draws-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    DatePipe,
    SlicePipe,
    MatButtonModule,
    MatIconModule,
    TchLoading,
    TchErrorPanel,
    AdminPageHeader,
    AdminEmptyState,
  ],
  templateUrl: './admin-draws.page.html',
  styleUrl: './admin-draws.page.scss',
})
export class AdminDrawsPage {
  private readonly api = inject(DrawAdminApi);

  readonly FILTERS: readonly FilterOption[] = [
    { value: 'upcoming', label: 'À venir' },
    { value: 'past',     label: 'Passés' },
    { value: 'all',      label: 'Tous' },
  ];

  readonly activeFilter = signal<Filter>('upcoming');
  readonly page = signal(0);

  private readonly params = computed(() => ({
    filter: this.activeFilter(),
    page: this.page(),
  }));

  readonly state = toSignal(
    toObservable(this.params).pipe(
      switchMap(({ filter, page }) => {
        const today = new Date().toISOString().slice(0, 10);
        const req$ = filter === 'upcoming'
          ? this.api.listUpcoming(14)
          : this.api.listDraws({
              to: filter === 'past' ? today : undefined,
              page,
              size: PAGE_SIZE,
            });

        return req$.pipe(
          switchMap(p => of({
            status: 'ready',
            items: p.items,
            totalPages: p.totalPages,
          } as PageState)),
          catchError(() => of({ status: 'error' } as PageState)),
          startWith({ status: 'loading' } as PageState),
        );
      }),
    ),
    { initialValue: { status: 'loading' } as PageState },
  );

  statusLabel(status: DrawStatus): string {
    return STATUS_LABELS[status] ?? status;
  }

  setFilter(f: Filter): void {
    this.activeFilter.set(f);
    this.page.set(0);
  }

  prevPage(): void { this.page.update(p => Math.max(0, p - 1)); }
  nextPage(): void { this.page.update(p => p + 1); }
}
