import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';

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
  readonly name: string;
  readonly timezone: string;
  readonly drawTime: string;
  readonly cutoffSec: number | null;
  readonly daysOfWeek: readonly DrawChannelWeekDay[];
  readonly active: boolean;
  readonly period: string;
  readonly notes: string;
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
    TranslatePipe,
  ],
  templateUrl: './draw-channel-config.dialog.html',
  styleUrls: ['./draw-channel-config.dialog.scss'],
})
export class DrawChannelConfigDialog {
  private readonly api = inject(AdminDrawChannelsApiService);
  private readonly dialogRef = inject(MatDialogRef<DrawChannelConfigDialog, boolean>);
  private readonly data = inject<DrawChannelConfigDialogData>(MAT_DIALOG_DATA);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly errorKey = signal<string | null>(null);
  readonly detail = signal<DrawChannelDetailView | null>(null);
  readonly title = this.data.label;

  readonly weekDays: readonly DrawChannelWeekDay[] = [
    'MONDAY',
    'TUESDAY',
    'WEDNESDAY',
    'THURSDAY',
    'FRIDAY',
    'SATURDAY',
    'SUNDAY',
  ];

  readonly form = signal<DrawChannelConfigForm>({
    name: '',
    timezone: '',
    drawTime: '',
    cutoffSec: null,
    daysOfWeek: [],
    active: false,
    period: '',
    notes: '',
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorKey.set(null);
    this.api.getChannelDetail(this.data.channelId, { suppressShellFeedback: true }).subscribe({
      next: detail => {
        this.detail.set(detail);
        this.form.set({
          name: detail.name ?? '',
          timezone: detail.timezone ?? '',
          drawTime: normalizeTime(detail.drawTime),
          cutoffSec: detail.cutoffSec ?? null,
          daysOfWeek: detail.daysOfWeek ?? [],
          active: detail.active,
          period: detail.period ?? '',
          notes: detail.notes ?? '',
        });
        this.loading.set(false);
      },
      error: () => {
        this.errorKey.set('admin.drawChannels.config.error.load');
        this.loading.set(false);
      },
    });
  }

  patchForm(patch: Partial<DrawChannelConfigForm>): void {
    this.form.update(current => ({ ...current, ...patch }));
  }

  parseNumber(value: string): number | null {
    if (!value.trim()) return null;
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }

  save(): void {
    const detail = this.detail();
    if (!detail || this.saving()) return;
    const form = this.form();
    const request: UpdateTenantDrawChannelRequest = {
      name: form.name.trim() || detail.name,
      label: form.name.trim() || detail.label,
      timezone: form.timezone.trim() || null,
      drawTime: form.drawTime || null,
      cutoffSec: form.cutoffSec,
      daysOfWeek: form.daysOfWeek,
      active: form.active,
      sortOrder: detail.sortOrder,
      period: form.period.trim() || null,
      notes: form.notes.trim() || null,
      defaultSource: detail.defaultSource ?? null,
    };

    this.saving.set(true);
    this.errorKey.set(null);
    this.api.updateChannel(this.data.channelId, request, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogRef.close(true);
      },
      error: () => {
        this.errorKey.set('admin.drawChannels.config.error.save');
        this.saving.set(false);
      },
    });
  }
}

function normalizeTime(value: string | null | undefined): string {
  return value ? value.slice(0, 5) : '';
}
