import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AdminDialogShellComponent } from '@tch/ui/console';
import { webAppErrorFromProblemDetail } from '@tch/api';
import type { ProblemDetail } from '@tch/api';
import { resolveErrorFeedbackCopy, toErrorViewModel, ErrorViewModel } from '@tch/web/errors';
import { AdminDrawChannelsApiService } from '../../data-access/admin-draw-channels-api.service';
import {
  DrawChannelProviderView,
  DrawChannelSlotConfigView,
  DrawResultAcquisitionMode,
  UpdateDrawChannelProviderConfigRequest,
} from '../../data-access/admin-draw-channels.models';

export interface DrawChannelConfigDialogData {
  readonly provider: DrawChannelProviderView;
  readonly slot?: DrawChannelSlotConfigView | null;
}

interface SlotDraft {
  readonly slotKey: string;
  readonly label: string;
  readonly enabled: boolean;
  readonly drawTime: string;
  readonly salesCutoffMinutes: string;
}

interface ChannelDraft {
  readonly enabled: boolean;
  readonly resultAcquisitionMode: DrawResultAcquisitionMode;
  readonly defaultSalesCutoffMinutes: string;
  readonly slots: readonly SlotDraft[];
}

@Component({
  selector: 'tch-draw-channel-config-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminDialogShellComponent,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    TranslatePipe,
  ],
  templateUrl: './draw-channel-config.dialog.html',
  styleUrls: ['./draw-channel-config.dialog.scss'],
})
export class DrawChannelConfigDialog {
  protected readonly data = inject<DrawChannelConfigDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<DrawChannelConfigDialog>);
  private readonly api = inject(AdminDrawChannelsApiService);
  private readonly translate = inject(TranslateService);

  protected readonly acquisitionModes: readonly DrawResultAcquisitionMode[] = ['AUTO', 'MANUAL', 'UNCONFIGURED'];
  protected readonly saving = signal(false);
  protected readonly error = signal<ErrorViewModel | null>(null);
  protected readonly draft = signal<ChannelDraft>({
    enabled: this.data.provider.tenantStatus === 'ACTIVE' || this.data.provider.tenantStatus === 'NEEDS_CONFIG',
    resultAcquisitionMode: this.data.provider.resultAcquisition.mode,
    defaultSalesCutoffMinutes: this.toInput(this.data.provider.defaultSalesCutoffMinutes),
    slots: this.data.provider.slots.map(slot => ({
      slotKey: slot.slotKey,
      label: slot.label,
      enabled: slot.enabled,
      drawTime: slot.drawTime?.slice(0, 5) ?? '',
      salesCutoffMinutes: this.toInput(slot.salesCutoffMinutes),
    })),
  });
  protected readonly focusSlotKey = computed(() => this.data.slot?.slotKey ?? null);

  updateEnabled(enabled: boolean): void {
    this.patch({ enabled });
  }

  updateMode(resultAcquisitionMode: DrawResultAcquisitionMode): void {
    this.patch({ resultAcquisitionMode });
  }

  updateDefaultCutoff(defaultSalesCutoffMinutes: string): void {
    this.patch({ defaultSalesCutoffMinutes });
  }

  updateSlot(slotKey: string, patch: Partial<SlotDraft>): void {
    this.draft.update(current => ({
      ...current,
      slots: current.slots.map(slot => slot.slotKey === slotKey ? { ...slot, ...patch } : slot),
    }));
  }

  submit(): void {
    if (this.saving()) return;
    this.saving.set(true);
    this.error.set(null);
    this.api.updateDrawChannelProviderConfig(this.data.provider.providerCode, this.toRequest(this.draft()), {
      suppressShellFeedback: true,
    }).subscribe({
      next: updated => {
        this.saving.set(false);
        this.dialogRef.close(updated);
      },
      error: err => {
        this.saving.set(false);
        this.error.set(this.errorViewModel(err));
      },
    });
  }

  private patch(patch: Partial<ChannelDraft>): void {
    this.draft.update(current => ({ ...current, ...patch }));
  }

  private toRequest(value: ChannelDraft): UpdateDrawChannelProviderConfigRequest {
    return {
      enabled: value.enabled,
      resultAcquisitionMode: value.resultAcquisitionMode,
      defaultSalesCutoffMinutes: this.toNumber(value.defaultSalesCutoffMinutes),
      slots: value.slots.map(slot => ({
        slotKey: slot.slotKey,
        enabled: slot.enabled,
        drawTime: slot.enabled ? (slot.drawTime || null) : null,
        salesCutoffMinutes: this.toNumber(slot.salesCutoffMinutes),
      })),
    };
  }

  private toInput(value: number | null | undefined): string {
    return value === null || value === undefined ? '' : String(value);
  }

  private toNumber(value: string): number | null {
    const trimmed = value.trim();
    if (!trimmed) return null;
    const parsed = Number(trimmed);
    return Number.isFinite(parsed) ? parsed : null;
  }

  private errorViewModel(err: unknown): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (!problem) {
      return {
        title: this.translate.instant('common.errors.fallback.title'),
        message: this.translate.instant('common.errors.fallback.message'),
        severity: 'error',
      };
    }
    const normalized = webAppErrorFromProblemDetail(problem, 'admin.draw_channels.config', 'section');
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return toErrorViewModel(normalized, copy);
  }
}
