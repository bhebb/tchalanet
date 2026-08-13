import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { consoleDrawIdentity } from '@tch/web/console';

import { AdminDrawChannelsApiService } from '../../data-access/admin-draw-channels-api.service';
import {
  DrawChannelDetailView,
  DrawChannelSource,
  UpdateTenantDrawChannelRequest,
} from '../../data-access/admin-draw-channels.models';

interface DrawChannelConfigDialogData {
  readonly channelId: string;
  readonly label: string;
  readonly mode: 'configure' | 'details';
}

interface DrawChannelConfigForm {
  readonly cutoffSec: number | null;
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
  readonly errorKey = signal<string | null>(null);
  readonly detail = signal<DrawChannelDetailView | null>(null);
  readonly title = this.data.label;
  readonly mode = this.data.mode;

  readonly form = signal<DrawChannelConfigForm>({
    cutoffSec: null,
    active: false,
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
          cutoffSec: detail.cutoffSec ?? null,
          active: detail.active,
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

  titleKey(): string {
    return this.mode === 'details'
      ? 'admin.drawChannels.config.detailsTitle'
      : 'admin.drawChannels.config.title';
  }

  drawTimeLabel(detail: DrawChannelDetailView): string {
    return normalizeTime(detail.drawTime) || '—';
  }

  timezoneLabel(detail: DrawChannelDetailView): string {
    return detail.timezone?.trim() || '—';
  }

  resultModeKey(source: DrawChannelSource | null | undefined): string {
    return source === 'MANUAL'
      ? 'admin.drawChannels.config.result.mode.manual'
      : 'admin.drawChannels.config.result.mode.automatic';
  }

  resultSourceKey(source: DrawChannelSource | null | undefined): string {
    return source === 'MANUAL'
      ? 'admin.drawChannels.config.result.source.manual'
      : 'admin.drawChannels.config.result.source.provider';
  }

  resultProviderLabel(detail: DrawChannelDetailView): string {
    const provider = detail.resultProvider?.trim();
    if (!provider) return '—';
    return consoleDrawIdentity({ providerCode: provider }).providerName ?? provider;
  }

  resultSlotLabel(detail: DrawChannelDetailView): string {
    return detail.resultProviderSlotCode?.trim() || detail.resultSlotKey?.trim() || '—';
  }

  resultDaysLabel(detail: DrawChannelDetailView): string {
    const value = detail.resultSlotDaysOfWeek?.trim();
    if (!value) return '—';
    if (value === 'MON-SUN') {
      return this.translate.instant('admin.drawChannels.list.days.everyDay');
    }
    return value;
  }

  resultProjectionKey(detail: DrawChannelDetailView): string {
    return detail.resultSlotActive === false
      ? 'admin.drawChannels.config.result.projection.inactive'
      : 'admin.drawChannels.config.result.projection.active';
  }

  save(): void {
    const detail = this.detail();
    if (!detail || this.saving() || this.mode === 'details') return;
    const form = this.form();
    const request: UpdateTenantDrawChannelRequest = {
      cutoffSec: form.cutoffSec,
      active: form.active,
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
