import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';

import { GamesAdminApiService, GameSettings, TenantGameView } from '../../../games-admin-api.service';

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
  ],
  template: `
    <h2 mat-dialog-title>Paramètres — {{ data.game.displayName }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="game-settings-dialog__form">
        <mat-checkbox formControlName="visibleInPos">Visible au POS</mat-checkbox>

        <mat-form-field appearance="outline" class="game-settings-dialog__field">
          <mat-label>Mise minimum</mat-label>
          <input matInput type="number" formControlName="minStake" />
        </mat-form-field>

        <mat-form-field appearance="outline" class="game-settings-dialog__field">
          <mat-label>Mise maximum</mat-label>
          <input matInput type="number" formControlName="maxStake" />
        </mat-form-field>

        <mat-form-field appearance="outline" class="game-settings-dialog__field">
          <mat-label>Ordre d'affichage</mat-label>
          <input matInput type="number" formControlName="displayOrder" />
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="submitting()">Annuler</button>
      <button mat-flat-button color="primary" [disabled]="submitting()" (click)="submit()">
        @if (submitting()) {
          <span class="material-symbols-outlined spin" aria-hidden="true">progress_activity</span>
        }
        Enregistrer
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .game-settings-dialog__form { display: flex; flex-direction: column; gap: 0.75rem; width: 100%; min-width: 320px; }
    .game-settings-dialog__field { width: 100%; }
    .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class GameSettingsDialog {
  protected readonly data = inject<{ game: TenantGameView }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<GameSettingsDialog>);
  private readonly api = inject(GamesAdminApiService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);

  readonly form = this.fb.group({
    visibleInPos: [this.data.game.settings?.visibleInPos ?? true],
    minStake: [this.data.game.settings?.minStake ?? null],
    maxStake: [this.data.game.settings?.maxStake ?? null],
    displayOrder: [this.data.game.settings?.displayOrder ?? null],
  });

  submit(): void {
    if (this.submitting()) return;
    this.submitting.set(true);

    const v = this.form.value;
    const settings: GameSettings = {
      visibleInPos: v.visibleInPos ?? true,
      minStake: v.minStake ?? undefined,
      maxStake: v.maxStake ?? undefined,
      displayOrder: v.displayOrder ?? undefined,
    };

    this.api.updateGameSettings(this.data.game.gameCode, settings).subscribe({
      next: () => {
        this.submitting.set(false);
        this.snackBar.open('Paramètres mis à jour.', 'OK', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.snackBar.open(pd?.title ?? 'Erreur lors de la mise à jour.', 'OK', { duration: 4000 });
      },
    });
  }
}
