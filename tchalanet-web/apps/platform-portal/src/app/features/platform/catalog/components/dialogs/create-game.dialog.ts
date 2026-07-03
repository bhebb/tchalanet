import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormField, form, required, submit } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';
import { TchSectionError } from '@tch/ui/components';
import { tchMutation } from '@tch/web/async';

import {
  CatalogGameView,
  CreateGameRequest,
  PlatformCatalogApi,
} from '../../data-access/platform-catalog-api.service';

interface CreateGameFormModel {
  readonly code: string;
  readonly name: string;
  readonly category: string;
  readonly sortOrder: number;
  readonly active: boolean;
}

@Component({
  selector: 'tch-create-game-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormField,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    TchSectionError,
    TranslatePipe,
  ],
  templateUrl: './create-game.dialog.html',
  styleUrl: './catalog-dialog.scss',
})
export class CreateGameDialog {
  private readonly api = inject(PlatformCatalogApi);
  private readonly dialogRef = inject(MatDialogRef<CreateGameDialog>);

  readonly categories = ['LOTTO', 'PICK', 'MARRIAGE'] as const;
  readonly model = signal<CreateGameFormModel>({
    code: '',
    name: '',
    category: '',
    sortOrder: 10,
    active: true,
  });
  readonly form = form(this.model, path => {
    required(path.code, { message: 'platform.catalog.games.validation.codeRequired' });
    required(path.name, { message: 'platform.catalog.games.validation.nameRequired' });
  });

  readonly saveGame = tchMutation<CreateGameRequest, CatalogGameView>({
    source: 'platform.catalog.games.create',
    run: input => this.api.createGame(input),
    onSuccess: created => this.dialogRef.close(created),
  });
  readonly feedback = computed(() => this.saveGame.feedback());

  submit(event: Event): void {
    event.preventDefault();
    submit(this.form, async () => {
      this.saveGame.execute(this.toRequest(this.model()));
    });
  }

  private toRequest(value: CreateGameFormModel): CreateGameRequest {
    return {
      code: value.code.trim().toUpperCase(),
      name: value.name.trim(),
      category: value.category || null,
      active: value.active,
      sortOrder: value.sortOrder,
    };
  }
}
