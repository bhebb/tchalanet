import {
  ChangeDetectionStrategy,
  Component,
  inject,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, map, of, startWith } from 'rxjs';
import {
  TchLoading,
  TchErrorPanel,
  AdminPageHeader,
  AdminEmptyState,
} from '@tch/ui/components';
import {
  consoleBetOptionLabel,
  consoleBetTypeLabel,
  consoleGameIdentity,
} from '@tch/web/console';

import { BaremesAdminApi, type PricingOddsEntry } from './data-access/baremes-admin.api.service';

export interface GameGroup {
  readonly gameCode: string;
  readonly gameLabel: string;
  readonly logoText: string;
  readonly logoUrl: string | null;
  readonly rows: readonly PricingOddsEntry[];
}

type PageState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly groups: readonly GameGroup[] };

function groupByGame(entries: PricingOddsEntry[]): GameGroup[] {
  const map = new Map<string, PricingOddsEntry[]>();
  for (const e of entries.filter(e => e.active)) {
    const rows = map.get(e.gameCode) ?? [];
    rows.push(e);
    map.set(e.gameCode, rows);
  }
  return Array.from(map.entries()).map(([code, rows]) => {
    const game = consoleGameIdentity(code);
    return {
      gameCode: code,
      gameLabel: game.name,
      logoText: game.logoText,
      logoUrl: game.logoUrl,
      rows,
    };
  });
}

@Component({
  selector: 'tch-admin-baremes-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TchLoading,
    TchErrorPanel,
    AdminPageHeader,
    AdminEmptyState,
  ],
  templateUrl: './admin-baremes.page.html',
  styleUrl: './admin-baremes.page.scss',
})
export class AdminBaremesPage {
  private readonly api = inject(BaremesAdminApi);

  readonly state = toSignal(
    this.api.listTenantOdds().pipe(
      map(entries => ({
        status: 'ready',
        groups: groupByGame(entries),
      }) as PageState),
      catchError(() => of({ status: 'error' } as PageState)),
      startWith({ status: 'loading' } as PageState),
    ),
    { initialValue: { status: 'loading' } as PageState },
  );

  betTypeLabel(betType: string): string {
    return consoleBetTypeLabel(betType);
  }

  optionLabel(betType: string, betOption: number | null): string {
    return consoleBetOptionLabel(betType, betOption) ?? '';
  }

  payoutValue(row: PricingOddsEntry): string {
    if (row.payoutRuleType === 'FIXED_AMOUNT') {
      return row.fixedAmount === null || row.fixedAmount === undefined ? '—' : `${row.fixedAmount}`;
    }
    return row.odds === null || row.odds === undefined ? '—' : `×${row.odds}`;
  }
}
