import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';

import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import {
  GamesAdminApiService,
  TenantGameView,
  CatalogGameView,
  GameSettings,
} from '../../games-admin-api.service';

// ── Game Settings Dialog ───────────────────────────────────────────────────────

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
      <form [formGroup]="form" class="dialog-form">
        <mat-checkbox formControlName="visibleInPos">Visible au POS</mat-checkbox>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Mise minimum</mat-label>
          <input matInput type="number" formControlName="minStake" />
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Mise maximum</mat-label>
          <input matInput type="number" formControlName="maxStake" />
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Ordre d'affichage</mat-label>
          <input matInput type="number" formControlName="displayOrder" />
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="submitting()">Annuler</button>
      <button mat-flat-button color="primary" [disabled]="submitting()" (click)="submit()">
        @if (submitting()) {
          <span class="material-symbols-outlined spin">progress_activity</span>
        }
        Enregistrer
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .dialog-form { display: flex; flex-direction: column; gap: 0.75rem; min-width: 360px; }
      .full-width { width: 100%; }
      .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
      @keyframes spin { to { transform: rotate(360deg); } }
    `,
  ],
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
      error: () => {
        this.submitting.set(false);
        this.snackBar.open('Erreur lors de la mise à jour.', 'OK', { duration: 4000 });
      },
    });
  }
}

// ── Main page ─────────────────────────────────────────────────────────────────

@Component({
  selector: 'tch-admin-games-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTabsModule,
  ],
  template: `
    <tch-admin-page-shell title="Jeux" description="Gestion des jeux activés pour ce tenant.">
      <mat-tab-group>
        <!-- Jeux activés -->
        <mat-tab label="Jeux activés">
          @if (loadingGames()) {
            <div class="loading-state">
              <span class="material-symbols-outlined spin">progress_activity</span>
              Chargement...
            </div>
          } @else if (errorGames()) {
            <div class="error-panel">
              <span class="material-symbols-outlined">error</span>
              {{ errorGames() }}
            </div>
          } @else if (games().length === 0) {
            <tch-admin-empty-state
              icon="casino"
              title="Aucun jeu activé"
              message="Activez des jeux depuis le catalogue."
            />
          } @else {
            <table mat-table [dataSource]="games()">
              <ng-container matColumnDef="gameCode">
                <th mat-header-cell *matHeaderCellDef>Code</th>
                <td mat-cell *matCellDef="let row">{{ row.gameCode }}</td>
              </ng-container>
              <ng-container matColumnDef="displayName">
                <th mat-header-cell *matHeaderCellDef>Nom</th>
                <td mat-cell *matCellDef="let row">{{ row.displayName }}</td>
              </ng-container>
              <ng-container matColumnDef="enabled">
                <th mat-header-cell *matHeaderCellDef>Activé</th>
                <td mat-cell *matCellDef="let row">
                  <button
                    mat-stroked-button
                    [color]="row.enabled ? 'warn' : 'primary'"
                    (click)="toggleGame(row)"
                  >
                    {{ row.enabled ? 'Désactiver' : 'Activer' }}
                  </button>
                </td>
              </ng-container>
              <ng-container matColumnDef="settings">
                <th mat-header-cell *matHeaderCellDef></th>
                <td mat-cell *matCellDef="let row">
                  <button mat-icon-button (click)="openSettings(row)">
                    <span class="material-symbols-outlined">tune</span>
                  </button>
                </td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="gameColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: gameColumns"></tr>
            </table>
          }
        </mat-tab>

        <!-- Catalogue -->
        <mat-tab label="Catalogue">
          @if (loadingCatalog()) {
            <div class="loading-state">
              <span class="material-symbols-outlined spin">progress_activity</span>
              Chargement...
            </div>
          } @else if (errorCatalog()) {
            <div class="error-panel">
              <span class="material-symbols-outlined">error</span>
              {{ errorCatalog() }}
            </div>
          } @else if (catalog().length === 0) {
            <tch-admin-empty-state
              icon="inventory"
              title="Catalogue vide"
              message="Aucun jeu disponible dans le catalogue."
            />
          } @else {
            <table mat-table [dataSource]="catalog()">
              <ng-container matColumnDef="gameCode">
                <th mat-header-cell *matHeaderCellDef>Code</th>
                <td mat-cell *matCellDef="let row">{{ row.gameCode }}</td>
              </ng-container>
              <ng-container matColumnDef="displayName">
                <th mat-header-cell *matHeaderCellDef>Nom</th>
                <td mat-cell *matCellDef="let row">{{ row.displayName }}</td>
              </ng-container>
              <ng-container matColumnDef="category">
                <th mat-header-cell *matHeaderCellDef>Catégorie</th>
                <td mat-cell *matCellDef="let row">{{ row.category }}</td>
              </ng-container>
              <ng-container matColumnDef="description">
                <th mat-header-cell *matHeaderCellDef>Description</th>
                <td mat-cell *matCellDef="let row">{{ row.description ?? '—' }}</td>
              </ng-container>
              <ng-container matColumnDef="activate">
                <th mat-header-cell *matHeaderCellDef></th>
                <td mat-cell *matCellDef="let row">
                  @if (!isEnabled(row.gameCode)) {
                    <button mat-stroked-button color="primary" (click)="enableFromCatalog(row)">
                      <span class="material-symbols-outlined">add</span>
                      Activer
                    </button>
                  }
                </td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="catalogColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: catalogColumns"></tr>
            </table>
          }
        </mat-tab>
      </mat-tab-group>
    </tch-admin-page-shell>
  `,
  styles: [
    `
      .loading-state {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 2rem;
        color: var(--tch-color-on-surface-variant);
      }
      .spin { animation: spin 0.8s linear infinite; display: inline-block; }
      @keyframes spin { to { transform: rotate(360deg); } }
      .error-panel {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 1rem;
        border-radius: 0.5rem;
        background: var(--tch-color-error-container, #ffdad6);
        color: var(--tch-color-on-error-container, #410002);
        margin: 1rem 0;
      }
      table { width: 100%; }
    `,
  ],
})
export class AdminGamesPage implements OnInit {
  private readonly api = inject(GamesAdminApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly gameColumns = ['gameCode', 'displayName', 'enabled', 'settings'];
  readonly catalogColumns = ['gameCode', 'displayName', 'category', 'description', 'activate'];

  readonly loadingGames = signal(false);
  readonly errorGames = signal<string | null>(null);
  readonly games = signal<TenantGameView[]>([]);

  readonly loadingCatalog = signal(false);
  readonly errorCatalog = signal<string | null>(null);
  readonly catalog = signal<CatalogGameView[]>([]);

  ngOnInit(): void {
    this.loadGames();
    this.loadCatalog();
  }

  private loadGames(): void {
    this.loadingGames.set(true);
    this.api.listEnabledGames().subscribe({
      next: v => { this.games.set(v); this.loadingGames.set(false); },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.errorGames.set(pd?.title ?? 'Erreur.');
        this.loadingGames.set(false);
      },
    });
  }

  private loadCatalog(): void {
    this.loadingCatalog.set(true);
    this.api.listCatalogGames().subscribe({
      next: v => { this.catalog.set(v); this.loadingCatalog.set(false); },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.errorCatalog.set(pd?.title ?? 'Erreur.');
        this.loadingCatalog.set(false);
      },
    });
  }

  isEnabled(gameCode: string): boolean {
    return this.games().some(g => g.gameCode === gameCode && g.enabled);
  }

  toggleGame(game: TenantGameView): void {
    const op = game.enabled ? this.api.disableGame(game.gameCode) : this.api.enableGame(game.gameCode);
    op.subscribe({
      next: () => {
        this.snackBar.open(`Jeu ${game.gameCode} ${game.enabled ? 'désactivé' : 'activé'}.`, 'OK', { duration: 3000 });
        this.loadGames();
      },
      error: () => this.snackBar.open('Erreur.', 'OK', { duration: 4000 }),
    });
  }

  enableFromCatalog(game: CatalogGameView): void {
    this.api.enableGame(game.gameCode).subscribe({
      next: () => {
        this.snackBar.open(`Jeu ${game.gameCode} activé.`, 'OK', { duration: 3000 });
        this.loadGames();
      },
      error: () => this.snackBar.open('Erreur lors de l\'activation.', 'OK', { duration: 4000 }),
    });
  }

  openSettings(game: TenantGameView): void {
    const ref = this.dialog.open(GameSettingsDialog, { data: { game }, width: '440px' });
    ref.afterClosed().subscribe(ok => {
      if (ok) this.loadGames();
    });
  }
}
