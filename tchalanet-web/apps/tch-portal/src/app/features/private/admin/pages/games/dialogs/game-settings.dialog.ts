import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchErrorPanel } from '@tch/ui/components';

import { resolveErrorFeedbackCopy } from '../../../../../../core/api/error-feedback-copy';
import { ErrorViewModel, toErrorViewModel } from '../../../../../../core/api/local-error-routing';
import { GamesAdminApiService, UpdateGameSettingsRequest, TenantGameView } from '../../../games-admin-api.service';

@Component({
  selector: 'tch-game-settings-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    TchErrorPanel,
  ],
  templateUrl: './game-settings.dialog.html',
  styleUrls: ['./game-settings.dialog.scss'],
})
export class GameSettingsDialog {
  protected readonly data = inject<{ game: TenantGameView }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<GameSettingsDialog>);
  private readonly api = inject(GamesAdminApiService);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly submitting = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);

  readonly form = this.fb.group({
    displayName: [this.data.game.displayName ?? ''],
    visibleInPos: [this.data.game.visibleInPos],
    minStake: [this.data.game.minStake],
    maxStake: [this.data.game.maxStake],
    displayOrder: [this.data.game.displayOrder],
    availabilityEnabled: [this.data.game.availabilityEnabled],
    startLocalTime: [this.data.game.startLocalTime ?? ''],
    endLocalTime: [this.data.game.endLocalTime ?? ''],
  });

  submit(): void {
    if (this.submitting()) return;
    this.submitting.set(true);
    this.error.set(null);

    const v = this.form.getRawValue();
    const req: UpdateGameSettingsRequest = {
      displayName: v.displayName || null,
      visibleInPos: v.visibleInPos,
      minStake: v.minStake,
      maxStake: v.maxStake,
      displayOrder: v.displayOrder,
      availabilityEnabled: v.availabilityEnabled,
      startLocalTime: v.availabilityEnabled ? (v.startLocalTime || null) : null,
      endLocalTime: v.availabilityEnabled ? (v.endLocalTime || null) : null,
    };

    this.api.updateGameSettings(this.data.game.gameCode, req, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.dialogRef.close(true);
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(this.errorViewModel(err));
      },
    });
  }

  private errorViewModel(err: unknown): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, 'admin.games.settings', 'page');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return toErrorViewModel(normalized, copy);
    }

    return {
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    };
  }
}
