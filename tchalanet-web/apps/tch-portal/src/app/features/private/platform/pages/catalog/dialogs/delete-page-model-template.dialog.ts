import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { PageModelTemplateView } from '../../../platform-pagemodels-api.service';

@Component({
  selector: 'tch-delete-page-model-template-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatDialogModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>Supprimer le template</h2>
    <mat-dialog-content>
      <p>Voulez-vous vraiment supprimer <strong class="mono">{{ data.code }}</strong> ?</p>
      <p class="warning-text">Cette action est irréversible.</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">Annuler</button>
      <button mat-flat-button color="warn" [mat-dialog-close]="true">Supprimer</button>
    </mat-dialog-actions>
  `,
  styles: [`.mono { font-family: monospace; } .warning-text { color: var(--tch-color-error); font-size: 0.875rem; }`],
})
export class DeletePageModelTemplateDialog {
  protected readonly data = inject<PageModelTemplateView>(MAT_DIALOG_DATA);
}
