import {
  ChangeDetectionStrategy,
  Component,
  inject,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, map, of, startWith } from 'rxjs';
import {
  TchLoading,
  TchErrorPanel,
  AdminPageHeader,
  AdminEmptyState,
} from '@tch/ui/components';

import { BaremesAdminApi, type PricingOddsEntry } from '../../baremes-admin.api.service';

// Display labels for each bet type
const BET_TYPE_LABELS: Record<string, string> = {
  MATCH_1_2D: '1er lot',
  MATCH_2_2D: '2e lot',
  MATCH_3_2D: '3e lot',
  MARRIAGE_2D2D: 'Mariage',
  LOTTO3_3D: 'Loto 3',
  LOTTO4_PATTERN: 'Loto 4',
  LOTTO5_PATTERN: 'Loto 5',
};

// Display labels for bet options
const BET_OPTION_LABELS: Record<string, Record<number, string>> = {
  MARRIAGE_2D2D: { 1: 'Ordre exact', 2: 'Revers / Double' },
  LOTTO3_3D: { 1: 'Exact', 2: 'Désordre / Box' },
  LOTTO4_PATTERN: { 1: 'Exact', 2: 'Désordre / Box', 3: '2 premiers', 4: '2 derniers' },
  LOTTO5_PATTERN: { 1: 'Lot1 + Lot2', 2: 'Lot1 + Lot3', 3: 'Mixte' },
};

const GAME_LABELS: Record<string, string> = {
  HT_BOLET: 'Borlette',
  HT_MARYAJ: 'Mariage',
  HT_MARYAJ_GRATUIT: 'Mariage Gratuit',
  HT_LOTO3: 'Loto 3',
  HT_LOTO4: 'Loto 4',
  HT_LOTO5: 'Loto 5',
};

export interface GameGroup {
  readonly gameCode: string;
  readonly gameLabel: string;
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
  return Array.from(map.entries()).map(([code, rows]) => ({
    gameCode: code,
    gameLabel: GAME_LABELS[code] ?? code,
    rows,
  }));
}

@Component({
  selector: 'tch-admin-baremes-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    MatIconModule,
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
    return BET_TYPE_LABELS[betType] ?? betType;
  }

  optionLabel(betType: string, betOption: number | null): string {
    if (betOption == null) return '';
    return BET_OPTION_LABELS[betType]?.[betOption] ?? `Option ${betOption}`;
  }
}
