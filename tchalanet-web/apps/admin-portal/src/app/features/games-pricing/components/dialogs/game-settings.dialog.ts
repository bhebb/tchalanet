import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormField, form, submit as submitForm } from '@angular/forms/signals';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe } from '@ngx-translate/core';
import { TchSectionError } from '@tch/ui/components';
import { AdminDialogShellComponent } from '@tch/ui/console';
import { tchMutation } from '@tch/web/async';

import { ConsoleBetLabelPipe, ConsoleGameNamePipe } from '@tch/web/console';
import { GamesAdminApiService, UpdateGameSettingsRequest, TenantGameView } from '../../../games-pricing/data-access/games-admin-api.service';

interface GameSettingsFormModel {
  readonly displayName: string;
  readonly visibleInPos: boolean;
  readonly minStake: number | null;
  readonly maxStake: number | null;
  readonly displayOrder: number;
  readonly availabilityEnabled: boolean;
  readonly startLocalTime: string;
  readonly endLocalTime: string;
}

@Component({
  selector: 'tch-game-settings-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormField,
    AdminDialogShellComponent,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    TchSectionError,
    TranslatePipe,
    ConsoleBetLabelPipe,
    ConsoleGameNamePipe,
  ],
  templateUrl: './game-settings.dialog.html',
  styleUrls: ['./game-settings.dialog.scss'],
})
export class GameSettingsDialog {
  protected readonly data = inject<{ game: TenantGameView }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<GameSettingsDialog>);
  private readonly api = inject(GamesAdminApiService);

  readonly model = signal<GameSettingsFormModel>({
    displayName: this.data.game.displayName ?? '',
    visibleInPos: this.data.game.visibleInPos,
    minStake: this.data.game.minStake,
    maxStake: this.data.game.maxStake,
    displayOrder: this.data.game.displayOrder,
    availabilityEnabled: this.data.game.availabilityEnabled,
    startLocalTime: this.data.game.startLocalTime ?? '',
    endLocalTime: this.data.game.endLocalTime ?? '',
  });
  readonly form = form(this.model);

  readonly saveSettings = tchMutation<UpdateGameSettingsRequest, void>({
    source: 'admin.games.settings',
    run: req => this.api.updateGameSettings(this.data.game.gameCode, req, { suppressShellFeedback: true }),
    onSuccess: () => this.dialogRef.close(true),
  });
  readonly feedback = computed(() => this.saveSettings.feedback());

  submit(event: Event): void {
    event.preventDefault();
    submitForm(this.form, async () => {
      this.saveSettings.execute(this.toRequest(this.model()));
    });
  }

  private toRequest(value: GameSettingsFormModel): UpdateGameSettingsRequest {
    return {
      displayName: value.displayName || null,
      visibleInPos: value.visibleInPos,
      minStake: value.minStake,
      maxStake: value.maxStake,
      displayOrder: value.displayOrder,
      availabilityEnabled: value.availabilityEnabled,
      startLocalTime: value.availabilityEnabled ? (value.startLocalTime || null) : null,
      endLocalTime: value.availabilityEnabled ? (value.endLocalTime || null) : null,
    };
  }
}
