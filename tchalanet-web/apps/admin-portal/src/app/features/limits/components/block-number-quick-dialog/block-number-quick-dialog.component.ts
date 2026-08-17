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
   * Generic locked target — when set, the scope radio is hidden and the dialog
   * operates in context mode (no scope or channel picker shown).
   */
  readonly lockedTargetType?: 'TENANT' | 'DRAW_CHANNEL' | 'SELLER_TERMINAL';
  readonly lockedTargetId?: string;
  readonly lockedTargetLabel?: string;
  /** Legacy — kept for backwards compat; prefer lockedTargetType/Id/Label. */
  readonly channelId?: string;
  readonly channelLabel?: string;
}

type Scope = 'TENANT' | 'DRAW_CHANNEL' | 'SELLER_TERMINAL';

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

  // Resolve locked target — prefer new generic fields, fall back to legacy channelId/channelLabel
  readonly lockedTargetType = signal<Scope | null>(
    this.data?.lockedTargetType ?? (this.data?.channelId ? 'DRAW_CHANNEL' : null),
  );
  readonly lockedTargetId = signal<string | null>(
    this.data?.lockedTargetId ?? this.data?.channelId ?? null,
  );
  readonly lockedTargetLabel = signal<string | null>(
    this.data?.lockedTargetLabel ?? this.data?.channelLabel ?? null,
  );
  readonly hasLockedTarget = computed(() => this.lockedTargetType() !== null);

  // Legacy compat aliases used by template
  readonly lockedChannelId = computed(() =>
    this.lockedTargetType() === 'DRAW_CHANNEL' ? this.lockedTargetId() : null,
  );
  readonly hasLockedChannel = computed(() => this.lockedTargetType() === 'DRAW_CHANNEL');

  readonly scope = signal<Scope>(
    this.data?.lockedTargetType ?? (this.data?.channelId ? 'DRAW_CHANNEL' : 'DRAW_CHANNEL'),
  );

  // Channel pre-selected from the dialog's context (e.g., draw detail page).
  readonly preselectedChannelId = signal<string | null>(
    this.data?.lockedTargetType === 'DRAW_CHANNEL'
      ? (this.data.lockedTargetId ?? null)
      : (this.data?.channelId ?? null),
  );

  // Channel chosen by the user in the picker (may be different from pre-selection).
  readonly selectedChannel = signal<DrawChannelSummary | null>(null);

  readonly canSave = computed(() => {
    if (this.selections().length === 0) return false;
    if (this.scope() === 'DRAW_CHANNEL') {
      return this.hasLockedTarget() || this.selectedChannel() !== null;
    }
    return true; // TENANT and SELLER_TERMINAL (always locked) just need selections
  });

  readonly descriptionKey = computed(() =>
    this.hasLockedTarget()
      ? 'admin.limits.blockNumber.contextDescription'
      : 'admin.limits.blockNumber.quickDescription',
  );

  readonly contextIcon = computed<string>(() => {
    switch (this.lockedTargetType()) {
      case 'DRAW_CHANNEL': return 'event';
      case 'SELLER_TERMINAL': return 'person';
      case 'TENANT': return 'domain';
      default: return 'shield';
    }
  });

  readonly contextLabelKey = computed(() => {
    if (this.lockedTargetType() === 'SELLER_TERMINAL') {
      return 'admin.limits.blockNumber.contextLabelSeller';
    }
    if (this.lockedTargetType() === 'TENANT') {
      return 'admin.limits.blockNumber.contextLabelTenant';
    }
    return 'admin.limits.blockNumber.contextLabel';
  });

  readonly contextHintKey = computed(() => {
    if (this.lockedTargetType() === 'SELLER_TERMINAL') {
      return 'admin.limits.blockNumber.contextHintSeller';
    }
    if (this.lockedTargetType() === 'TENANT') {
      return 'admin.limits.blockNumber.contextHintTenant';
    }
    return 'admin.limits.blockNumber.contextHint';
  });

  readonly contextFallbackKey = computed(() => {
    if (this.lockedTargetType() === 'SELLER_TERMINAL') {
      return 'admin.limits.blockNumber.contextFallbackSeller';
    }
    return 'admin.limits.blockNumber.contextFallback';
  });

  readonly saveMutation = tchMutation<void, { id: { value: string } }>({
    run: () => {
      const lockedType = this.lockedTargetType();
      const targetType = lockedType ?? (this.scope() === 'TENANT' ? 'TENANT' : 'DRAW_CHANNEL');
      const targetId =
        targetType === 'TENANT'
          ? undefined
          : (this.lockedTargetId() ?? this.selectedChannel()?.id ?? undefined);
      return this.api.upsertAssignment(
        {
          ruleKey: 'BLOCK_SELECTION_PER_DRAW',
          targetType,
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
    if (this.hasLockedTarget()) return;
    this.scope.set(scope);
    if (scope === 'TENANT') {
      this.selectedChannel.set(null);
    }
  }

  onChannelChange(channel: DrawChannelSummary | null): void {
    this.selectedChannel.set(channel);
  }

  addNumber(event: MatChipInputEvent): void {
    const val = (event.value ?? '').trim();
    if (/^\d+$/.test(val) && !this.selections().includes(val)) {
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
