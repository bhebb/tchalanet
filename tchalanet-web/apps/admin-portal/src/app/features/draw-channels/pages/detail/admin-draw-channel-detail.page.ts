import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { mapHttpErrorToProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import {
  AdminSectionCardComponent,
  AdminStatusPillComponent,
  TchIdentityCardComponent,
  type AdminStatusTone,
  type TchIdentityCardMeta,
} from '@tch/ui/console';
import {
  ConsoleEntityDetailComponent,
  ConsoleFactsComponent,
  type ConsoleEntityDetailActionEvent,
  type ConsoleFact,
  type ConsoleRowAction,
  consoleDrawIdentity,
} from '@tch/web/console';
import { resolveErrorFeedbackCopy, toErrorViewModel, type ErrorViewModel } from '@tch/web/errors';

import { DrawChannelConfigDialog } from '../../components/draw-channel-config-dialog/draw-channel-config.dialog';
import { AdminDrawChannelsApiService } from '../../data-access/admin-draw-channels-api.service';
import {
  type DrawChannelDetailView,
  type DrawChannelSource,
  type DrawChannelWeekDay,
} from '../../data-access/admin-draw-channels.models';

type PageState = 'loading' | 'ready' | 'error';

@Component({
  selector: 'tch-admin-draw-channel-detail-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    MatIconModule,
    TranslatePipe,
    AdminSectionCardComponent,
    AdminStatusPillComponent,
    TchIdentityCardComponent,
    ConsoleEntityDetailComponent,
    ConsoleFactsComponent,
  ],
  templateUrl: './admin-draw-channel-detail.page.html',
  styleUrls: ['./admin-draw-channel-detail.page.scss'],
})
export class AdminDrawChannelDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(AdminDrawChannelsApiService);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly pageState = signal<PageState>('loading');
  readonly channel = signal<DrawChannelDetailView | null>(null);
  readonly error = signal<ErrorViewModel | null>(null);

  readonly title = computed(() => this.channelTitle(this.channel()));
  readonly description = computed(() => this.channelDescription(this.channel()));
  readonly meta = computed(() => {
    const channel = this.channel();
    if (!channel) return [];
    return [
      channel.code,
      channel.timezone,
      channel.active
        ? this.t('admin.drawChannels.list.status.active')
        : this.t('admin.drawChannels.list.status.inactive'),
    ].filter(Boolean) as string[];
  });
  readonly actions = computed<readonly ConsoleRowAction[]>(() => [
    {
      id: 'back',
      label: this.t('admin.drawChannels.detail.actions.back'),
      icon: 'arrow_back',
    },
    {
      id: 'configure',
      label: this.t('admin.drawChannels.actions.configure'),
      icon: 'settings',
      tone: 'primary' as const,
    },
    {
      id: 'draws',
      label: this.t('admin.drawChannels.actions.viewGeneratedDraws'),
      icon: 'event',
    },
  ]);
  readonly statusTone = computed<AdminStatusTone>(() =>
    this.channel()?.active ? 'success' : 'neutral',
  );
  readonly statusLabel = computed(() =>
    this.channel()?.active
      ? this.t('admin.drawChannels.list.status.active')
      : this.t('admin.drawChannels.list.status.inactive'),
  );
  readonly salesFacts = computed<readonly ConsoleFact[]>(() => {
    const channel = this.channel();
    if (!channel) return [];
    return [
      {
        label: this.t('admin.drawChannels.config.active'),
        value: this.statusLabel(),
      },
      {
        label: this.t('admin.drawChannels.config.salesOpenTime'),
        value: normalizeTime(channel.salesOpenTime) || '—',
      },
      {
        label: this.t('admin.drawChannels.config.salesCloseTime'),
        value: salesCloseTime(channel.drawTime, channel.cutoffSec) || '—',
      },
      {
        label: this.t('admin.drawChannels.detail.fields.salesStop'),
        value: this.salesStopLabel(channel),
      },
      {
        label: this.t('admin.drawChannels.config.daysOfWeek'),
        value: this.daysLabel(channel.daysOfWeek),
      },
      {
        label: this.t('admin.drawChannels.config.notes'),
        value: channel.notes?.trim() || '—',
      },
    ];
  });
  readonly drawFacts = computed<readonly ConsoleFact[]>(() => {
    const channel = this.channel();
    if (!channel) return [];
    return [
      { label: this.t('admin.drawChannels.config.code'), value: channel.code, code: true },
      { label: this.t('admin.drawChannels.config.name'), value: channel.name },
      { label: this.t('admin.drawChannels.config.period'), value: channel.period ?? '—' },
      {
        label: this.t('admin.drawChannels.config.drawTime'),
        value: normalizeTime(channel.drawTime) || '—',
      },
      { label: this.t('admin.drawChannels.config.timezone'), value: channel.timezone ?? '—' },
      {
        label: this.t('admin.drawChannels.detail.fields.sortOrder'),
        value: channel.sortOrder ?? '—',
      },
    ];
  });
  readonly resultFacts = computed<readonly ConsoleFact[]>(() => {
    const channel = this.channel();
    if (!channel) return [];
    return [
      {
        label: this.t('admin.drawChannels.config.result.mode.label'),
        value: this.resultModeLabel(channel.defaultSource),
      },
      {
        label: this.t('admin.drawChannels.config.result.source.label'),
        value: this.resultSourceLabel(channel.defaultSource, channel.resultProvider),
      },
      {
        label: this.t('admin.drawChannels.config.result.providerSlot'),
        value: channel.resultProviderSlotCode?.trim() || channel.resultSlotKey?.trim() || '—',
        code: true,
      },
      {
        label: this.t('admin.drawChannels.config.result.openDays'),
        value: this.resultSlotDaysLabel(channel.resultSlotDaysOfWeek),
      },
      {
        label: this.t('admin.drawChannels.config.result.projection.label'),
        value:
          channel.resultSlotActive === false
            ? this.t('admin.drawChannels.config.result.projection.inactive')
            : this.t('admin.drawChannels.config.result.projection.active'),
      },
    ];
  });
  readonly asideMeta = computed<readonly TchIdentityCardMeta[]>(() => {
    const channel = this.channel();
    if (!channel) return [];
    return [
      {
        label: this.t('admin.drawChannels.config.drawTime'),
        value: normalizeTime(channel.drawTime) || '—',
      },
      {
        label: this.t('admin.drawChannels.config.salesCloseTime'),
        value: salesCloseTime(channel.drawTime, channel.cutoffSec) || '—',
      },
      {
        label: this.t('admin.drawChannels.config.result.mode.label'),
        value: this.resultModeLabel(channel.defaultSource),
      },
      {
        label: this.t('admin.drawChannels.config.daysOfWeek'),
        value: this.daysLabel(channel.daysOfWeek),
      },
    ];
  });

  constructor() {
    this.load();
  }

  load(): void {
    const channelId = this.route.snapshot.paramMap.get('id');
    if (!channelId) {
      this.pageState.set('error');
      this.error.set({
        title: this.t('admin.drawChannels.detail.error.missingTitle'),
        message: this.t('admin.drawChannels.detail.error.missingMessage'),
        severity: 'error',
      });
      return;
    }

    this.pageState.set('loading');
    this.error.set(null);
    this.api.getChannelDetail(channelId, { suppressShellFeedback: true }).subscribe({
      next: channel => {
        this.channel.set(channel);
        this.pageState.set('ready');
      },
      error: err => {
        this.error.set(this.errorViewModel(err));
        this.pageState.set('error');
      },
    });
  }

  onDetailAction(event: ConsoleEntityDetailActionEvent): void {
    if (event.action.id === 'back') {
      void this.router.navigate(['/app/admin/draw-channels']);
      return;
    }
    if (event.action.id === 'configure') {
      this.openConfig();
      return;
    }
    if (event.action.id === 'draws') {
      this.openGeneratedDraws();
    }
  }

  openConfig(): void {
    const channel = this.channel();
    if (!channel) return;
    this.dialog
      .open(DrawChannelConfigDialog, {
        data: {
          channelId: channel.id,
          label: this.channelTitle(channel),
          mode: 'configure',
        },
        maxWidth: '640px',
        width: 'min(640px, 96vw)',
      })
      .afterClosed()
      .subscribe(saved => {
        if (saved === true) this.load();
      });
  }

  openGeneratedDraws(): void {
    const channel = this.channel();
    if (!channel) return;
    const identity = consoleDrawIdentity({
      channelCode: channel.code,
      channelName: channel.name,
      localTimeLabel: channel.drawTime ?? undefined,
    });
    void this.router.navigate(['/app/admin/draws'], {
      queryParams: {
        provider: identity.providerCode ?? providerCodeFromChannel(channel.code),
        slotKey: identity.slotKey ?? channel.code,
      },
    });
  }

  channelTitle(channel: DrawChannelDetailView | null): string {
    if (!channel) return this.t('admin.drawChannels.detail.titleFallback');
    const identity = consoleDrawIdentity({
      channelCode: channel.code,
      channelName: channel.name,
      localTimeLabel: channel.drawTime ?? undefined,
    });
    const label = channel.label?.trim();
    return identity.channelName ?? (label || channel.name);
  }

  channelDescription(channel: DrawChannelDetailView | null): string {
    if (!channel) return '';
    return this.t('admin.drawChannels.detail.description', {
      code: channel.code,
      timezone: channel.timezone ?? '—',
    });
  }

  private salesStopLabel(channel: DrawChannelDetailView): string {
    const draw = minutesOfDay(normalizeTime(channel.drawTime));
    const close = minutesOfDay(salesCloseTime(channel.drawTime, channel.cutoffSec));
    if (draw == null || close == null || close >= draw) return '—';
    return this.t('admin.drawChannels.config.salesStopBeforeDraw', {
      minutes: draw - close,
      drawTime: formatDrawTimeForSalesNote(
        normalizeTime(channel.drawTime),
        this.translate.currentLang,
      ),
    });
  }

  private daysLabel(days: readonly DrawChannelWeekDay[] | null | undefined): string {
    if (!days?.length) return '—';
    if (days.length === DAY_ORDER.length) return this.t('admin.drawChannels.list.days.everyDay');
    return days.map(day => this.t(`admin.drawChannels.days.${day}`)).join(', ');
  }

  private resultSlotDaysLabel(value: string | null | undefined): string {
    const days = expandResultSlotDays(value);
    if (!days.length || days.length === DAY_ORDER.length) {
      return this.t('admin.drawChannels.list.days.everyDay');
    }
    return days.map(day => this.t(`admin.drawChannels.days.${day}`)).join(', ');
  }

  private resultModeLabel(source: DrawChannelSource | null | undefined): string {
    return source === 'MANUAL'
      ? this.t('admin.drawChannels.config.result.mode.manual')
      : this.t('admin.drawChannels.config.result.mode.automatic');
  }

  private resultSourceLabel(
    source: DrawChannelSource | null | undefined,
    provider: string | null | undefined,
  ): string {
    if (source === 'MANUAL') return this.t('admin.drawChannels.config.result.source.manual');
    const label = provider?.trim() || '—';
    return this.t('admin.drawChannels.config.result.source.provider', { provider: label });
  }

  private errorViewModel(err: unknown): ErrorViewModel {
    const problem = mapHttpErrorToProblemDetail(err);
    const normalized = webAppErrorFromProblemDetail(problem, 'admin.drawChannels.detail', 'page');
    const copy = resolveErrorFeedbackCopy(normalized, key => this.t(key));
    return toErrorViewModel(normalized, copy);
  }

  private t(key: string, params?: Record<string, string | number>): string {
    return this.translate.instant(key, params);
  }
}

function normalizeTime(value: string | null | undefined): string {
  return value ? value.slice(0, 5) : '';
}

function salesCloseTime(
  drawTime: string | null | undefined,
  cutoffSec: number | null | undefined,
): string {
  const draw = minutesOfDay(normalizeTime(drawTime));
  if (draw == null || cutoffSec == null) return '';
  return timeOfDay(draw - Math.round(cutoffSec / 60));
}

function minutesOfDay(value: string): number | null {
  const [hourValue, minuteValue] = value.split(':');
  const hour = Number(hourValue);
  const minute = Number(minuteValue);
  if (!Number.isInteger(hour) || !Number.isInteger(minute)) return null;
  if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return null;
  return hour * 60 + minute;
}

function timeOfDay(totalMinutes: number): string {
  const minutes = ((totalMinutes % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY;
  const hour = Math.floor(minutes / 60);
  const minute = minutes % 60;
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
}

function formatDrawTimeForSalesNote(value: string, language: string | undefined): string {
  const minutes = minutesOfDay(value);
  if (minutes == null) return value;
  const hour24 = Math.floor(minutes / 60);
  const minute = minutes % 60;
  if (language?.startsWith('ht')) {
    const period = hour24 < 12 ? 'maten' : 'aswè';
    const hour12 = hour24 % 12 || 12;
    return minute === 0
      ? `${hour12}zè ${period}`
      : `${hour12}:${String(minute).padStart(2, '0')} ${period}`;
  }
  if (language?.startsWith('en')) {
    const period = hour24 < 12 ? 'AM' : 'PM';
    const hour12 = hour24 % 12 || 12;
    return `${hour12}:${String(minute).padStart(2, '0')} ${period}`;
  }
  return value;
}

function providerCodeFromChannel(channelCode: string): string {
  return channelCode.trim().split(/[_-]/)[0]?.toUpperCase() || channelCode;
}

function expandResultSlotDays(value: string | null | undefined): readonly DrawChannelWeekDay[] {
  const source = value?.trim().toUpperCase();
  if (!source || source === 'MON-SUN') return DAY_ORDER;

  const days = new Set<DrawChannelWeekDay>();
  for (const part of source.split(',')) {
    const token = part.trim();
    if (!token) continue;
    if (token.includes('-')) {
      const [start, end] = token.split('-').map(segment => segment.trim());
      for (const day of daysInRange(start, end)) days.add(day);
    } else {
      const day = dayToken(token);
      if (day) days.add(day);
    }
  }
  return DAY_ORDER.filter(day => days.has(day));
}

function dayToken(token: string): DrawChannelWeekDay | null {
  return DAY_TOKEN_TO_CODE[token.slice(0, 3)] ?? null;
}

function daysInRange(
  startToken: string | undefined,
  endToken: string | undefined,
): readonly DrawChannelWeekDay[] {
  const start = dayToken(startToken ?? '');
  const end = dayToken(endToken ?? '');
  if (!start || !end) return [];
  const startIndex = DAY_ORDER.indexOf(start);
  const endIndex = DAY_ORDER.indexOf(end);
  const result: DrawChannelWeekDay[] = [];
  for (let index = startIndex; ; index = (index + 1) % DAY_ORDER.length) {
    result.push(DAY_ORDER[index]);
    if (index === endIndex) break;
  }
  return result;
}

const MINUTES_PER_DAY = 24 * 60;

const DAY_ORDER: readonly DrawChannelWeekDay[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];

const DAY_TOKEN_TO_CODE: Record<string, DrawChannelWeekDay> = {
  MON: 'MONDAY',
  TUE: 'TUESDAY',
  WED: 'WEDNESDAY',
  THU: 'THURSDAY',
  FRI: 'FRIDAY',
  SAT: 'SATURDAY',
  SUN: 'SUNDAY',
};
