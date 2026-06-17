import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
} from '@angular/core';
import { SlicePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, of, startWith, switchMap } from 'rxjs';
import { forkJoin } from 'rxjs';
import {
  TchLoading,
  TchErrorPanel,
  AdminPageHeader,
  AdminStatusBadge,
  AdminEmptyState,
} from '@tch/ui/components';

import {
  DrawAdminApi,
  DrawChannelDetail,
  DrawChannelSummary,
  DayOfWeek,
  GameSummary,
  ChannelGames,
} from '../../draw-admin.api.service';

interface DayEntry {
  readonly key: DayOfWeek;
  readonly short: string;
}

type ListState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly channels: DrawChannelSummary[]; readonly gameMap: Map<string, GameSummary[]> };

type DetailState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly channel: DrawChannelDetail };

@Component({
  selector: 'tch-admin-draw-channels-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    SlicePipe,
    MatIconModule,
    TchLoading,
    TchErrorPanel,
    AdminPageHeader,
    AdminStatusBadge,
    AdminEmptyState,
  ],
  templateUrl: './admin-draw-channels.page.html',
  styleUrl: './admin-draw-channels.page.scss',
})
export class AdminDrawChannelsPage {
  private readonly api = inject(DrawAdminApi);

  readonly ALL_DAYS: readonly DayEntry[] = [
    { key: 'MONDAY',    short: 'L' },
    { key: 'TUESDAY',   short: 'M' },
    { key: 'WEDNESDAY', short: 'M' },
    { key: 'THURSDAY',  short: 'J' },
    { key: 'FRIDAY',    short: 'V' },
    { key: 'SATURDAY',  short: 'S' },
    { key: 'SUNDAY',    short: 'D' },
  ];

  readonly selectedCode = signal<string | null>(null);

  readonly state = toSignal(
    forkJoin({
      channels: this.api.listChannels(),
      channelGames: this.api.getChannelGames(),
    }).pipe(
      switchMap(({ channels, channelGames }) => {
        const gameMap = new Map<string, GameSummary[]>(
          channelGames.map((cg: ChannelGames) => [cg.channelCode, cg.games]),
        );
        return of({ status: 'ready', channels, gameMap } as ListState);
      }),
      catchError(() => of({ status: 'error' } as ListState)),
      startWith({ status: 'loading' } as ListState),
    ),
    { initialValue: { status: 'loading' } as ListState },
  );

  readonly detailState = toSignal(
    toObservable(this.selectedCode).pipe(
      switchMap(code =>
        code
          ? this.api.getChannelByCode(code).pipe(
              switchMap(channel => of({ status: 'ready', channel } as DetailState)),
              catchError(() => of({ status: 'error' } as DetailState)),
              startWith({ status: 'loading' } as DetailState),
            )
          : of({ status: 'loading' } as DetailState),
      ),
    ),
    { initialValue: { status: 'loading' } as DetailState },
  );

  select(code: string): void {
    this.selectedCode.update(current => current === code ? null : code);
  }

  gamesForChannel(channelCode: string): GameSummary[] {
    const vm = this.state();
    if (vm.status !== 'ready') return [];
    return vm.gameMap.get(channelCode) ?? [];
  }
}
