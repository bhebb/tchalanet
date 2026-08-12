import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
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
import { COMMA, ENTER } from '@angular/cdk/keycodes';
import type { MatChipInputEvent } from '@angular/material/chips';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { mapHttpErrorToProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchSectionError } from '@tch/ui/components';
import { AdminDialogShellComponent } from '@tch/ui/console';
import { resolveErrorFeedbackCopy, toErrorViewModel } from '@tch/web/errors';

import { AdminLimitsApi } from '../../data-access/admin-limits-api.service';

export interface BlockNumberQuickDialogData {
  readonly channelId?: string;
}

@Component({
  selector: 'tch-block-number-quick-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    AdminDialogShellComponent,
    MatButtonModule,
    MatChipsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    TchSectionError,
    TranslatePipe,
  ],
  template: `
    <tch-admin-dialog-shell
      title="Bloke nimero"
      description="Ajoute les numéros sélectionnés à la liste de blocage du canal."
      icon="block"
    >
      <div class="bnqd">
        @if (error()) {
          <tch-section-error title="Erreur" [message]="error()!" />
        }

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
        [disabled]="saving() || selections().length === 0"
        (click)="save()"
      >
        @if (saving()) {
          <span class="material-symbols-outlined" aria-hidden="true" style="animation: spin 1s linear infinite">
            progress_activity
          </span>
        }
        Bloquer
      </button>
    </tch-admin-dialog-shell>
  `,
  styles: [`
    .bnqd {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .bnqd__help {
      font-size: 0.875rem;
      color: var(--tch-color-on-surface-variant, #46464f);
      margin: 0;
    }

    .bnqd__field {
      width: 100%;
    }

    @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
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
    const channelId = this.data.channelId;
    if (!channelId || this.selections().length === 0) return;

    this.saving.set(true);
    this.error.set(null);

    this.api
      .upsertAssignment(
        {
          ruleKey: 'BLOCK_SELECTION_PER_DRAW',
          targetType: 'DRAW_CHANNEL',
          targetId: channelId,
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
