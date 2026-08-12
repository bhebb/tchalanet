import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { COMMA, ENTER } from '@angular/cdk/keycodes';
import type { MatChipInputEvent } from '@angular/material/chips';
import { TranslateService } from '@ngx-translate/core';

import { mapHttpErrorToProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchSectionError } from '@tch/ui/components';
import { AdminDialogShellComponent } from '@tch/ui/console';
import { resolveErrorFeedbackCopy, toErrorViewModel } from '@tch/web/errors';

import type { DrawChannelSummary } from '../../../draws/components/active-draw-channel-select/active-draw-channel-select.component';
import { ActiveDrawChannelSelectComponent } from '../../../draws/components/active-draw-channel-select/active-draw-channel-select.component';
import { AdminLimitsApi } from '../../data-access/admin-limits-api.service';

export interface BlockNumberQuickDialogData {
  /**
   * When provided (e.g., opened from a draw detail page), the dialog pre-selects
   * this draw channel and defaults to DRAW_CHANNEL scope.
   * When absent, the dialog defaults to TENANT (global) scope.
   */
  readonly channelId?: string;
}

type Scope = 'TENANT' | 'DRAW_CHANNEL';

@Component({
  selector: 'tch-block-number-quick-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
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
  template: `
    <tch-admin-dialog-shell
      title="Bloke nimero"
      description="Ajoute les numéros sélectionnés à la liste de blocage."
      icon="block"
    >
      <div class="bnqd">
        @if (error()) {
          <tch-section-error title="Erreur" [message]="error()!" />
        }

        <!-- Scope toggle -->
        <div class="bnqd__scope">
          <p class="bnqd__scope-label">Portée du blocage</p>
          <mat-radio-group [ngModel]="scope()" (ngModelChange)="setScope($event)" class="bnqd__radio-group">
            <mat-radio-button value="TENANT" class="bnqd__radio">
              <span class="bnqd__radio-title">Tout le tenant</span>
              <span class="bnqd__radio-hint">Bloque le numéro sur tous les canaux</span>
            </mat-radio-button>
            <mat-radio-button value="DRAW_CHANNEL" class="bnqd__radio">
              <span class="bnqd__radio-title">Canal spécifique</span>
              <span class="bnqd__radio-hint">Bloque le numéro sur un canal de tirage</span>
            </mat-radio-button>
          </mat-radio-group>
        </div>

        <!-- Draw channel picker (only when DRAW_CHANNEL scope) -->
        @if (scope() === 'DRAW_CHANNEL') {
          <tch-active-draw-channel-select
            [preselectedId]="preselectedChannelId()"
            (channelChange)="onChannelChange($event)"
          />
        }

        <!-- Number input -->
        <p class="bnqd__help">
          Entrez les numéros à bloquer. Appuyez sur Entrée ou virgule pour ajouter chaque numéro.
        </p>

        <mat-form-field appearance="outline" class="bnqd__field">
          <mat-label>Numéros</mat-label>
          <mat-chip-grid #chipGrid aria-label="Numéros à bloquer">
            @for (num of selections(); track num) {
              <mat-chip-row (removed)="removeNumber(num)">
                {{ num }}
                <button matChipRemove aria-label="Retirer {{ num }}">
                  <span class="material-symbols-outlined" aria-hidden="true">cancel</span>
                </button>
              </mat-chip-row>
            }
          </mat-chip-grid>
          <input
            placeholder="Ex: 12, 34..."
            [matChipInputFor]="chipGrid"
            [matChipInputSeparatorKeyCodes]="separatorKeyCodes"
            (matChipInputTokenEnd)="addNumber($event)"
          />
        </mat-form-field>
      </div>

      <button actions mat-button mat-dialog-close [disabled]="saving()">
        Annuler
      </button>
      <button
        actions
        mat-flat-button
        color="primary"
        [disabled]="saving() || !canSave()"
        (click)="save()"
      >
        @if (saving()) {
          <span class="material-symbols-outlined spin" aria-hidden="true">progress_activity</span>
        }
        Bloquer
      </button>
    </tch-admin-dialog-shell>
  `,
  styles: [`
    .bnqd {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .bnqd__scope-label {
      margin: 0 0 0.5rem;
      font-size: 0.8125rem;
      font-weight: 600;
      color: var(--tch-color-on-surface-variant);
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }

    .bnqd__radio-group {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .bnqd__radio {
      display: flex;
      align-items: flex-start;
      padding: 0.5rem 0.75rem;
      border: 1px solid var(--tch-color-outline-variant);
      border-radius: var(--tch-radius-md, 12px);
      transition: border-color 0.15s, background 0.15s;
    }

    .bnqd__radio:has(mat-radio-button.mat-mdc-radio-checked),
    .bnqd__radio:has(input[type="radio"]:checked) {
      border-color: var(--tch-color-primary);
      background: color-mix(in srgb, var(--tch-color-primary-container) 40%, transparent);
    }

    .bnqd__radio-title {
      display: block;
      font-weight: 700;
      font-size: 0.9rem;
      color: var(--tch-color-on-surface);
      line-height: 1.3;
    }

    .bnqd__radio-hint {
      display: block;
      font-size: 0.775rem;
      color: var(--tch-color-on-surface-variant);
      line-height: 1.4;
    }

    .bnqd__help {
      font-size: 0.875rem;
      color: var(--tch-color-on-surface-variant);
      margin: 0;
    }

    .bnqd__field { width: 100%; }

    .spin {
      animation: spin 1s linear infinite;
    }

    @keyframes spin {
      from { transform: rotate(0deg); }
      to   { transform: rotate(360deg); }
    }

    .material-symbols-outlined {
      font-family: 'Material Symbols Outlined';
    }
  `],
})
export class BlockNumberQuickDialogComponent {
  private readonly api = inject(AdminLimitsApi);
  private readonly dialogRef = inject(MatDialogRef<BlockNumberQuickDialogComponent>);
  private readonly data = inject<BlockNumberQuickDialogData>(MAT_DIALOG_DATA);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  readonly separatorKeyCodes = [ENTER, COMMA];
  readonly selections = signal<string[]>([]);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  // Always default to DRAW_CHANNEL. The user can switch to TENANT if needed.
  readonly scope = signal<Scope>('DRAW_CHANNEL');

  // Channel pre-selected from the dialog's context (e.g., draw detail page).
  readonly preselectedChannelId = signal<string | null>(this.data.channelId ?? null);

  // Channel chosen by the user in the picker (may be different from pre-selection).
  readonly selectedChannel = signal<DrawChannelSummary | null>(null);

  readonly canSave = computed(() => {
    if (this.selections().length === 0) return false;
    if (this.scope() === 'DRAW_CHANNEL') return this.selectedChannel() !== null;
    return true;
  });

  setScope(scope: Scope): void {
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
    if (!this.canSave()) return;

    const isTenant = this.scope() === 'TENANT';
    const targetId = isTenant ? undefined : (this.selectedChannel()?.id ?? undefined);

    this.saving.set(true);
    this.error.set(null);

    this.api
      .upsertAssignment(
        {
          ruleKey: 'BLOCK_SELECTION_PER_DRAW',
          targetType: isTenant ? 'TENANT' : 'DRAW_CHANNEL',
          ...(targetId ? { targetId } : {}),
          enabled: true,
          onBreach: 'BLOCK',
          params: { selections: this.selections() },
        },
        { suppressShellFeedback: true },
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: result => this.dialogRef.close(result),
        error: (err: unknown) => {
          this.error.set(this.resolveError(err));
          this.saving.set(false);
        },
      });
  }

  private resolveError(err: unknown): string {
    const problem = mapHttpErrorToProblemDetail(err);
    const normalized = webAppErrorFromProblemDetail(problem, 'admin.limits.blockNumber', 'section');
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return toErrorViewModel(normalized, copy).message;
  }
}
