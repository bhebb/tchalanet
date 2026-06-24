import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';

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
  ],
  template: `
    <h2 mat-dialog-title>Paramètres — {{ data.game.displayName ?? data.game.catalogName }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="game-settings-dialog__form">
        <mat-form-field appearance="outline" class="game-settings-dialog__field">
          <mat-label>Nom affiché</mat-label>
          <input matInput formControlName="displayName" [placeholder]="data.game.catalogName" />
        </mat-form-field>

        <div class="game-settings-dialog__row">
          <mat-form-field appearance="outline" class="game-settings-dialog__field">
            <mat-label>Mise minimum</mat-label>
            <input matInput type="number" formControlName="minStake" />
          </mat-form-field>
          <mat-form-field appearance="outline" class="game-settings-dialog__field">
            <mat-label>Mise maximum</mat-label>
            <input matInput type="number" formControlName="maxStake" />
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="game-settings-dialog__field">
          <mat-label>Ordre d'affichage</mat-label>
          <input matInput type="number" formControlName="displayOrder" />
        </mat-form-field>

        <mat-checkbox formControlName="visibleInPos">Visible au POS</mat-checkbox>
        <mat-checkbox formControlName="availabilityEnabled">Restreindre les horaires de vente</mat-checkbox>

        @if (form.value.availabilityEnabled) {
          <div class="game-settings-dialog__row">
            <mat-form-field appearance="outline" class="game-settings-dialog__field">
              <mat-label>Heure de début (HH:mm)</mat-label>
              <input matInput formControlName="startLocalTime" placeholder="08:00" />
            </mat-form-field>
            <mat-form-field appearance="outline" class="game-settings-dialog__field">
              <mat-label>Heure de fin (HH:mm)</mat-label>
              <input matInput formControlName="endLocalTime" placeholder="20:00" />
            </mat-form-field>
          </div>
        }
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
    .game-settings-dialog__form { display: flex; flex-direction: column; gap: 0.75rem; width: 100%; min-width: 360px; }
    .game-settings-dialog__field { width: 100%; }
    .game-settings-dialog__row { display: flex; gap: 0.75rem; }
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

    this.api.updateGameSettings(this.data.game.gameCode, req).subscribe({
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
