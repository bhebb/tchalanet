import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

import { SettingView } from '../../data-access/platform-settings-api.service';

@Component({
  selector: 'tch-delete-setting-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatDialogModule],
  template: `
    <h2 mat-dialog-title>Supprimer le paramètre</h2>
    <mat-dialog-content>
      <p>Voulez-vous vraiment supprimer <strong class="mono">{{ data.namespace }}.{{ data.settingKey }}</strong> ?</p>
      <p class="warning-text">Cette action est irréversible (soft delete).</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">Annuler</button>
      <button mat-flat-button color="warn" [mat-dialog-close]="true">Supprimer</button>
    </mat-dialog-actions>
  `,
  styles: [`.mono { font-family: monospace; } .warning-text { color: var(--tch-color-error); font-size: 0.875rem; }`],
})
export class DeleteSettingDialog {
  protected readonly data = inject<SettingView>(MAT_DIALOG_DATA);
}
