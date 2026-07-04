import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormField, form, required, submit } from '@angular/forms/signals';
import { MAT_DIALOG_DATA, MatDialogClose, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';
import { TchSectionError } from '@tch/ui/components';
import { AdminDialogShellComponent } from '@tch/ui/console';
import { tchMutation } from '@tch/web/async';

import {
  CatalogGameView,
  PlatformCatalogApi,
  UpdateGameRequest,
} from '../../data-access/platform-catalog-api.service';

interface EditGameFormModel {
  readonly name: string;
  readonly category: string;
  readonly sortOrder: number;
  readonly active: boolean;
}

@Component({
  selector: 'tch-edit-game-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormField,
    AdminDialogShellComponent,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogClose,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    TchSectionError,
    TranslatePipe,
  ],
  templateUrl: './edit-game.dialog.html',
})
export class EditGameDialog {
  private readonly api = inject(PlatformCatalogApi);
  private readonly dialogRef = inject(MatDialogRef<EditGameDialog>);
  readonly game = inject<CatalogGameView>(MAT_DIALOG_DATA);

  readonly categories = ['LOTTO', 'PICK', 'MARRIAGE'] as const;
  readonly model = signal<EditGameFormModel>({
    name: this.game.name,
    category: this.game.category ?? '',
    sortOrder: this.game.sortOrder,
    active: this.game.active,
  });
  readonly form = form(this.model, path => {
    required(path.name, { message: 'platform.catalog.games.validation.nameRequired' });
  });

  readonly saveGame = tchMutation<UpdateGameRequest, CatalogGameView>({
    source: 'platform.catalog.games.edit',
    run: input => this.api.updateGame(this.game.id, input),
    onSuccess: updated => this.dialogRef.close(updated),
  });
  readonly feedback = computed(() => this.saveGame.feedback());

  submit(event: Event): void {
    event.preventDefault();
    submit(this.form, async () => {
      this.saveGame.execute(this.toRequest(this.model()));
    });
  }

  private toRequest(value: EditGameFormModel): UpdateGameRequest {
    return {
      name: value.name.trim(),
      category: value.category || null,
      sortOrder: value.sortOrder,
      active: value.active,
    };
  }
}
