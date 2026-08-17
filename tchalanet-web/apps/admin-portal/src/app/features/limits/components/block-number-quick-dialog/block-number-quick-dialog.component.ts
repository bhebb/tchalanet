import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { COMMA, ENTER } from '@angular/cdk/keycodes';
import type { MatChipInputEvent } from '@angular/material/chips';
import { TranslatePipe } from '@ngx-translate/core';

import { TchSectionError } from '@tch/ui/components';
import { AdminDialogShellComponent } from '@tch/ui/console';
import { tchMutation } from '@tch/web/async';

import type { DrawChannelSummary } from '../../../draws/components/active-draw-channel-select/active-draw-channel-select.component';
import { ActiveDrawChannelSelectComponent } from '../../../draws/components/active-draw-channel-select/active-draw-channel-select.component';
import { AdminLimitsApi } from '../../data-access/admin-limits-api.service';

export interface BlockNumberQuickDialogData {
  /**
   * When provided (e.g., opened from a draw detail page), the dialog pre-selects
   * this draw channel and defaults to DRAW_CHANNEL scope.
   * When absent, the dialog lets the admin choose between tenant and channel scope.
   */
  readonly channelId?: string;
  readonly channelLabel?: string;
}

type Scope = 'TENANT' | 'DRAW_CHANNEL';

@Component({
  selector: 'tch-block-number-quick-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    TranslatePipe,
    ActiveDrawChannelSelectComponent,
    AdminDialogShellComponent,
    MatButtonModule,
    MatChipsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
    TchSectionError,
  ],
  templateUrl: './block-number-quick-dialog.component.html',
  styleUrl: './block-number-quick-dialog.component.scss',
})
export class BlockNumberQuickDialogComponent {
  private readonly api = inject(AdminLimitsApi);
  private readonly dialogRef = inject(MatDialogRef<BlockNumberQuickDialogComponent>);
  private readonly data = inject<BlockNumberQuickDialogData | null>(MAT_DIALOG_DATA);

  readonly separatorKeyCodes = [ENTER, COMMA];
  readonly selections = signal<string[]>([]);

  readonly lockedChannelId = signal<string | null>(this.data?.channelId ?? null);
  readonly lockedChannelLabel = signal<string | null>(this.data?.channelLabel ?? null);
  readonly hasLockedChannel = computed(() => this.lockedChannelId() !== null);

  // Always default to DRAW_CHANNEL. The user can switch to TENANT if needed.
  readonly scope = signal<Scope>('DRAW_CHANNEL');

  // Channel pre-selected from the dialog's context (e.g., draw detail page).
  readonly preselectedChannelId = signal<string | null>(this.data?.channelId ?? null);

  // Channel chosen by the user in the picker (may be different from pre-selection).
  readonly selectedChannel = signal<DrawChannelSummary | null>(null);

  readonly canSave = computed(() => {
    if (this.selections().length === 0) return false;
    if (this.scope() === 'DRAW_CHANNEL') {
      return this.hasLockedChannel() || this.selectedChannel() !== null;
    }
    return true;
  });

  readonly descriptionKey = computed(() =>
    this.hasLockedChannel()
      ? 'admin.limits.blockNumber.contextDescription'
      : 'admin.limits.blockNumber.quickDescription',
  );

  readonly saveMutation = tchMutation<void, { id: { value: string } }>({
    run: () => {
      const isTenant = this.scope() === 'TENANT';
      const targetId = isTenant
        ? undefined
        : (this.lockedChannelId() ?? this.selectedChannel()?.id ?? undefined);
      return this.api.upsertAssignment(
        {
          ruleKey: 'BLOCK_SELECTION_PER_DRAW',
          targetType: isTenant ? 'TENANT' : 'DRAW_CHANNEL',
          ...(targetId ? { targetId } : {}),
          enabled: true,
          onBreach: 'BLOCK',
          params: { selections: this.selections() },
        },
        { suppressShellFeedback: true },
      );
    },
    source: 'admin.limits.blockNumber',
    onSuccess: result => this.dialogRef.close(result),
  });

  setScope(scope: Scope): void {
    if (this.hasLockedChannel() && scope !== 'DRAW_CHANNEL') return;
    this.scope.set(scope);
    // Clearing the selection when switching to TENANT avoids stale channel state.
    if (scope === 'TENANT') {
      this.selectedChannel.set(null);
    }
  }

  onChannelChange(channel: DrawChannelSummary | null): void {
    this.selectedChannel.set(channel);
  }

  addNumber(event: MatChipInputEvent): void {
    const val = (event.value ?? '').trim();
    if (val && !this.selections().includes(val)) {
      this.selections.update(s => [...s, val]);
    }
    event.chipInput?.clear();
  }

  removeNumber(num: string): void {
    this.selections.update(s => s.filter(x => x !== num));
  }

  save(): void {
    if (!this.canSave() || this.saveMutation.pending()) return;
    this.saveMutation.execute();
  }
}
