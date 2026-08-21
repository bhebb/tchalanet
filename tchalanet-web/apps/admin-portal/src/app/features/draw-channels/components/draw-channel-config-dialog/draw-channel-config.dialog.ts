import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { TchSectionError } from '@tch/ui/components';
import { presentApiError, type ErrorViewModel } from '@tch/web/errors';

import { AdminDrawChannelsApiService } from '../../data-access/admin-draw-channels-api.service';
import {
  DrawChannelDetailView,
  DrawChannelWeekDay,
  UpdateTenantDrawChannelRequest,
} from '../../data-access/admin-draw-channels.models';

interface DrawChannelConfigDialogData {
  readonly channelId: string;
  readonly label: string;
}

interface DrawChannelConfigForm {
  readonly salesOpenTime: string;
  readonly salesCloseTime: string;
  readonly daysOfWeek: readonly DrawChannelWeekDay[];
  readonly notes: string;
  readonly active: boolean;
}

@Component({
  selector: 'tch-draw-channel-config-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    TchSectionError,
    TranslatePipe,
  ],
  templateUrl: './draw-channel-config.dialog.html',
  styleUrls: ['./draw-channel-config.dialog.scss'],
})
export class DrawChannelConfigDialog {
  private readonly api = inject(AdminDrawChannelsApiService);
  private readonly dialogRef = inject(MatDialogRef<DrawChannelConfigDialog, boolean>);
  private readonly data = inject<DrawChannelConfigDialogData>(MAT_DIALOG_DATA);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly detail = signal<DrawChannelDetailView | null>(null);
  readonly title = this.data.label;
  readonly supportedWeekDays = computed(() =>
    expandResultSlotDays(this.detail()?.resultSlotDaysOfWeek),
  );
  readonly salesOpenAfterDraw = computed(() => {
    const detail = this.detail();
    if (!detail) return false;
    const draw = minutesOfDay(normalizeTime(detail.drawTime));
    const open = minutesOfDay(this.form().salesOpenTime);
    return draw != null && open != null && open > draw;
  });
  readonly salesCloseAtDrawTime = computed(() => {
    const detail = this.detail();
    if (!detail) return false;
    const draw = minutesOfDay(normalizeTime(detail.drawTime));
    const close = minutesOfDay(this.form().salesCloseTime);
    return draw != null && close != null && close === draw;
  });
  readonly salesCloseAfterDraw = computed(() => {
    const detail = this.detail();
    if (!detail) return false;
    const draw = minutesOfDay(normalizeTime(detail.drawTime));
    const close = minutesOfDay(this.form().salesCloseTime);
    return draw != null && close != null && close > draw;
  });
  readonly salesStopNote = computed(() => {
    const detail = this.detail();
    if (!detail) return null;
    const drawTime = normalizeTime(detail.drawTime);
    const draw = minutesOfDay(drawTime);
    const close = minutesOfDay(this.form().salesCloseTime);
    if (draw == null || close == null || close >= draw) return null;
    return {
      minutes: draw - close,
      drawTime: formatDrawTimeForSalesNote(drawTime, this.translate.currentLang() ?? undefined),
    };
  });
  readonly canSave = computed(
    () =>
      this.form().daysOfWeek.length > 0 &&
      !this.salesOpenAfterDraw() &&
      !this.salesCloseAtDrawTime() &&
      !this.salesCloseAfterDraw(),
  );

  readonly form = signal<DrawChannelConfigForm>({
    salesOpenTime: '00:00',
    salesCloseTime: '',
    daysOfWeek: [],
    notes: '',
    active: false,
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.getChannelDetail(this.data.channelId, { suppressShellFeedback: true }).subscribe({
      next: detail => {
        this.detail.set(detail);
        const supportedDays = expandResultSlotDays(detail.resultSlotDaysOfWeek);
        this.form.set({
          salesOpenTime: normalizeTime(detail.salesOpenTime) || '00:00',
          salesCloseTime: salesCloseTime(detail.drawTime, detail.cutoffSec),
          daysOfWeek: supportedTenantDays(detail.daysOfWeek, supportedDays),
          notes: detail.notes ?? '',
          active: detail.active,
        });
        this.loading.set(false);
      },
      error: err => {
        this.error.set(this.errorViewModel(err, 'admin.drawChannels.config.load'));
        this.loading.set(false);
      },
    });
  }

  patchForm(patch: Partial<DrawChannelConfigForm>): void {
    this.form.update(current => ({ ...current, ...patch }));
  }

  titleKey(): string {
    return 'admin.drawChannels.config.title';
  }

  dayLabelKey(day: DrawChannelWeekDay): string {
    return `admin.drawChannels.days.${day}`;
  }

  resultDaysLabel(detail: DrawChannelDetailView): string {
    return this.daysLabel(expandResultSlotDays(detail.resultSlotDaysOfWeek));
  }

  daysLabel(days: readonly DrawChannelWeekDay[]): string {
    if (!days.length) return '—';
    if (days.length === DAY_ORDER.length) {
      return this.translate.instant('admin.drawChannels.list.days.everyDay');
    }
    return days.map(day => this.translate.instant(this.dayLabelKey(day))).join(', ');
  }

  save(): void {
    const detail = this.detail();
    if (!detail || this.saving() || !this.canSave()) return;
    const form = this.form();
    const request: UpdateTenantDrawChannelRequest = {
      salesOpenTime: form.salesOpenTime || null,
      cutoffSec: cutoffSeconds(detail.drawTime, form.salesCloseTime),
      daysOfWeek: form.daysOfWeek,
      notes: form.notes.trim() || null,
      active: form.active,
    };

    this.saving.set(true);
    this.error.set(null);
    this.api
      .updateChannel(this.data.channelId, request, { suppressShellFeedback: true })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.dialogRef.close(true);
        },
        error: err => {
          this.error.set(this.errorViewModel(err, 'admin.drawChannels.config.save'));
          this.saving.set(false);
        },
      });
  }

  private errorViewModel(err: unknown, source: string): ErrorViewModel {
    return presentApiError(err, key => this.translate.instant(key), {
      source,
      surface: 'section',
    }).viewModel;
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

function cutoffSeconds(drawTime: string | null | undefined, closeTime: string): number | null {
  const draw = minutesOfDay(normalizeTime(drawTime));
  const close = minutesOfDay(closeTime);
  if (draw == null || close == null) return null;
  return ((draw - close + MINUTES_PER_DAY) % MINUTES_PER_DAY) * 60;
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

function supportedTenantDays(
  tenantDays: readonly DrawChannelWeekDay[] | null | undefined,
  supportedDays: readonly DrawChannelWeekDay[],
): readonly DrawChannelWeekDay[] {
  const requested = tenantDays?.length ? tenantDays : supportedDays;
  const supported = new Set(supportedDays);
  return DAY_ORDER.filter(day => requested.includes(day) && supported.has(day));
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
