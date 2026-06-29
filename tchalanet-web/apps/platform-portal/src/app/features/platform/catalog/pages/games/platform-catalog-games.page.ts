import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatCheckboxModule } from '@angular/material/checkbox';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminDataToolbarComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminStatusPillComponent } from '@tch/ui/console';
import {
  PlatformCatalogApi,
  CatalogGameView,
  CreateGameRequest,
  UpdateGameRequest,
} from '../../data-access/platform-catalog-api.service';

// ── Create Dialog ────────────────────────────────────────────────────────────
@Component({
  selector: 'tch-create-game-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>Nouveau jeu</h2>
    <mat-dialog-content>
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:12px;padding-top:8px">
        <mat-form-field appearance="outline">
          <mat-label>Code</mat-label>
          <input matInput formControlName="code" placeholder="ex: BORLETTE" />
          @if (form.controls.code.invalid && form.controls.code.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Nom</mat-label>
          <input matInput formControlName="name" />
          @if (form.controls.name.invalid && form.controls.name.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Catégorie</mat-label>
          <mat-select formControlName="category">
            <mat-option value="">—</mat-option>
            <mat-option value="LOTTO">LOTTO</mat-option>
            <mat-option value="PICK">PICK</mat-option>
            <mat-option value="MARRIAGE">MARRIAGE</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Ordre d'affichage</mat-label>
          <input matInput type="number" formControlName="sortOrder" />
        </mat-form-field>
        <mat-checkbox formControlName="active">Actif</mat-checkbox>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button mat-dialog-close>Annuler</button>
      <button
        mat-flat-button
        color="primary"
        [disabled]="form.invalid || saving()"
        (click)="save()"
      >
        Créer
      </button>
    </mat-dialog-actions>
  `,
})
export class CreateGameDialog {
  private readonly api = inject(PlatformCatalogApi);
  private readonly ref = inject(MatDialogRef<CreateGameDialog>);
  private readonly fb = inject(FormBuilder);

  readonly saving = signal(false);
  readonly form = this.fb.nonNullable.group({
    code: ['', Validators.required],
    name: ['', Validators.required],
    category: [''],
    sortOrder: [10],
    active: [true],
  });

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
    const req: CreateGameRequest = {
      code: v.code.toUpperCase(),
      name: v.name,
      category: v.category || null,
      active: v.active,
      sortOrder: v.sortOrder,
    };
    this.api.createGame(req).subscribe({
      next: created => this.ref.close(created),
      error: (err: unknown) => { this.ref.close({ __error: (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.' }); },
    });
  }
}

// ── Edit Dialog ──────────────────────────────────────────────────────────────
@Component({
  selector: 'tch-edit-game-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>Modifier — {{ game().name }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:12px;padding-top:8px">
        <mat-form-field appearance="outline">
          <mat-label>Nom</mat-label>
          <input matInput formControlName="name" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Catégorie</mat-label>
          <mat-select formControlName="category">
            <mat-option value="">—</mat-option>
            <mat-option value="LOTTO">LOTTO</mat-option>
            <mat-option value="PICK">PICK</mat-option>
            <mat-option value="MARRIAGE">MARRIAGE</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Ordre d'affichage</mat-label>
          <input matInput type="number" formControlName="sortOrder" />
        </mat-form-field>
        <mat-checkbox formControlName="active">Actif</mat-checkbox>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button mat-dialog-close>Annuler</button>
      <button
        mat-flat-button
        color="primary"
        [disabled]="form.invalid || saving()"
        (click)="save()"
      >
        Enregistrer
      </button>
    </mat-dialog-actions>
  `,
})
export class EditGameDialog {
  private readonly api = inject(PlatformCatalogApi);
  private readonly ref = inject(MatDialogRef<EditGameDialog>);
  private readonly fb = inject(FormBuilder);

  readonly game = signal<CatalogGameView>({} as CatalogGameView);
  readonly saving = signal(false);

  readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    category: [''],
    sortOrder: [10],
    active: [true],
  });

  init(g: CatalogGameView): void {
    this.game.set(g);
    this.form.patchValue({
      name: g.name,
      category: g.category ?? '',
      sortOrder: g.sortOrder,
      active: g.active,
    });
  }

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
    const req: UpdateGameRequest = {
      name: v.name,
      category: v.category || null,
      sortOrder: v.sortOrder,
      active: v.active,
    };
    this.api.updateGame(this.game().id, req).subscribe({
      next: updated => this.ref.close(updated),
      error: (err: unknown) => { this.ref.close({ __error: (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.' }); },
    });
  }
}

// ── Main Page ────────────────────────────────────────────────────────────────
@Component({
  selector: 'tch-platform-catalog-games-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminStatusPillComponent,
    TchErrorPanel,
    TchLoading,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    ReactiveFormsModule,
  ],
  templateUrl: './platform-catalog-games.page.html',
})
export class PlatformCatalogGamesPage implements OnInit {
  private readonly api = inject(PlatformCatalogApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['code', 'name', 'category', 'sortOrder', 'active', 'actions'];

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly games = signal<CatalogGameView[]>([]);
  readonly search = signal('');
  readonly page = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);
  readonly hasNext = signal(false);
  readonly hasPrevious = signal(false);

  private showError(msg: string): void {
    this.error.set(msg);
    setTimeout(() => window.scrollTo({ top: 0, behavior: 'smooth' }), 50);
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listGames({ q: this.search() || undefined, page: this.page(), size: 20 }).subscribe({
      next: p => {
        this.games.set(p.items);
        this.totalElements.set(p.totalElements);
        this.page.set(p.page);
        this.totalPages.set(p.totalPages || 1);
        this.hasNext.set(p.hasNext ?? false);
        this.hasPrevious.set(p.hasPrevious ?? false);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(
          (err as { error?: { title?: string } })?.error?.title ?? 'Erreur de chargement.',
        );
        this.loading.set(false);
      },
    });
  }

  onSearch(v: string): void {
    this.search.set(v);
    this.page.set(0);
    this.load();
  }
  prevPage(): void {
    if (!this.hasPrevious()) return;
    this.page.set(this.page() - 1);
    this.load();
  }
  nextPage(): void {
    if (!this.hasNext()) return;
    this.page.set(this.page() + 1);
    this.load();
  }

  openCreate(): void {
    const ref = this.dialog.open(CreateGameDialog, { width: '480px' });
    ref.afterClosed().subscribe((created: CatalogGameView | { __error: string } | null) => {
      if (created && '__error' in created) { this.showError(created.__error); return; }
      if (created) { this.snackBar.open('Jeu créé.', 'OK', { duration: 4000 }); this.load(); }
    });
  }

  openEdit(game: CatalogGameView): void {
    const ref = this.dialog.open(EditGameDialog, { width: '480px' });
    (ref.componentInstance as EditGameDialog).init(game);
    ref.afterClosed().subscribe((updated: CatalogGameView | { __error: string } | null) => {
      if (updated && '__error' in updated) { this.showError(updated.__error); return; }
      if (updated) { this.snackBar.open('Jeu mis à jour.', 'OK', { duration: 4000 }); this.load(); }
    });
  }

  deactivate(game: CatalogGameView): void {
    if (!confirm(`Désactiver « ${game.name} » ?`)) return;
    this.api.deactivateGame(game.id).subscribe({
      next: () => {
        this.snackBar.open('Jeu désactivé.', 'OK', { duration: 4000 });
        this.load();
      },
      error: (err: unknown) => {
        this.snackBar.open(
          (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.',
          'OK',
          { duration: 5000 },
        );
      },
    });
  }

  delete(game: CatalogGameView): void {
    if (!confirm(`Supprimer définitivement « ${game.name} » ?`)) return;
    this.api.deleteGame(game.id).subscribe({
      next: () => {
        this.snackBar.open('Jeu supprimé.', 'OK', { duration: 4000 });
        this.load();
      },
      error: (err: unknown) => {
        this.snackBar.open(
          (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.',
          'OK',
          { duration: 5000 },
        );
      },
    });
  }
}
