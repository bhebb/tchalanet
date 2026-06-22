import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

import { PageModelTemplateView } from '../../../platform-pagemodels-api.service';

@Component({
  selector: 'tch-delete-pagemodel-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatDialogModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>Supprimer le template</h2>
    <mat-dialog-content>
      <p>Voulez-vous vraiment supprimer <strong class="delete-pagemodel-dialog__code">{{ data.code }}</strong> ?</p>
      <p class="delete-pagemodel-dialog__warning">Cette action est irréversible.</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">Annuler</button>
      <button mat-flat-button color="warn" [mat-dialog-close]="true">Supprimer</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .delete-pagemodel-dialog__code { font-family: monospace; }
    .delete-pagemodel-dialog__warning { color: var(--tch-color-error); font-size: 0.875rem; }
  `],
})
export class DeletePageModelDialog {
  protected readonly data = inject<PageModelTemplateView>(MAT_DIALOG_DATA);
}
