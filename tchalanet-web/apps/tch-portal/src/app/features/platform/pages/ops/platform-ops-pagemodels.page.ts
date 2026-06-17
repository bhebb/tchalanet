import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminCrudShellComponent } from '../../../private/shared/admin-ui/admin-crud-shell.component';
import { AdminDataToolbarComponent } from '../../../private/shared/admin-ui/admin-data-toolbar.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminStatusPillComponent, AdminStatusTone } from '../../../private/shared/admin-ui/admin-status-pill.component';
import {
  PageModelTemplateView,
  PlatformPageModelsApi,
} from '../../platform-pagemodels-api.service';

// ── Delete Confirm Dialog ──────────────────────────────────────────────────────

@Component({
  selector: 'tch-delete-pagemodel-dialog',
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
  styles: [
    `
      .mono { font-family: monospace; }
      .warning-text { color: var(--tch-color-error); font-size: 0.875rem; }
    `,
  ],
})
export class DeletePageModelDialog {
  protected readonly data = inject<PageModelTemplateView>(MAT_DIALOG_DATA);
}

// ── Main Page ─────────────────────────────────────────────────────────────────

@Component({
  selector: 'tch-platform-ops-pagemodels-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminStatusPillComponent,
    TchErrorPanel,
    TchLoading,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatTableModule,
  ],
  template: `
    <tch-admin-page-shell title="PageModel Templates" description="Gérez les templates de page : défaut, duplication, suppression.">
      <tch-admin-crud-shell>
        <div toolbar>
          <tch-admin-data-toolbar
            searchPlaceholder="Rechercher..."
            [searchValue]="search()"
            (searchChange)="onSearch($event)"
          >
            <mat-form-field appearance="outline" style="min-width:160px">
              <mat-label>Scope</mat-label>
              <input matInput [value]="scopeFilter()" (input)="onScope($event)" placeholder="ex: tenant" />
            </mat-form-field>
            <button mat-stroked-button (click)="load()">
              <span class="material-symbols-outlined">refresh</span>
            </button>
          </tch-admin-data-toolbar>
        </div>

        <div content>
          @if (loading()) {
            <tch-loading label="Chargement..." />
          } @else if (error()) {
            <tch-error-panel [title]="error()!" [showRetry]="true" retryLabel="Réessayer" (retry)="load()" />
          } @else if (templates().length === 0) {
            <tch-admin-empty-state icon="description" title="Aucun template" message="Aucun PageModel template trouvé." />
          } @else {
            <div class="table-container">
              <table mat-table [dataSource]="templates()">
                <ng-container matColumnDef="code">
                  <th mat-header-cell *matHeaderCellDef>Code</th>
                  <td mat-cell *matCellDef="let row"><span style="font-family:monospace">{{ row.code }}</span></td>
                </ng-container>
                <ng-container matColumnDef="logicalId">
                  <th mat-header-cell *matHeaderCellDef>ID Logique</th>
                  <td mat-cell *matCellDef="let row">{{ row.logicalId }}</td>
                </ng-container>
                <ng-container matColumnDef="scope">
                  <th mat-header-cell *matHeaderCellDef>Scope</th>
                  <td mat-cell *matCellDef="let row">{{ row.scope }}</td>
                </ng-container>
                <ng-container matColumnDef="name">
                  <th mat-header-cell *matHeaderCellDef>Nom</th>
                  <td mat-cell *matCellDef="let row">{{ row.name }}</td>
                </ng-container>
                <ng-container matColumnDef="level">
                  <th mat-header-cell *matHeaderCellDef>Niveau</th>
                  <td mat-cell *matCellDef="let row">
                    <tch-admin-status-pill [label]="row.level" [tone]="levelTone(row.level)" />
                  </td>
                </ng-container>
                <ng-container matColumnDef="isDefault">
                  <th mat-header-cell *matHeaderCellDef>Défaut</th>
                  <td mat-cell *matCellDef="let row">
                    @if (row.isDefault) {
                      <span class="material-symbols-outlined star-icon">star</span>
                    }
                  </td>
                </ng-container>
                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef></th>
                  <td mat-cell *matCellDef="let row">
                    <div class="action-row">
                      @if (!row.isDefault) {
                        <button mat-stroked-button (click)="setDefault(row)" [disabled]="busy()">
                          <span class="material-symbols-outlined">star</span>
                          Défaut
                        </button>
                      }
                      <button mat-stroked-button (click)="duplicate(row)" [disabled]="busy()">
                        <span class="material-symbols-outlined">content_copy</span>
                        Dupliquer
                      </button>
                      @if (!row.isDefault) {
                        <button mat-stroked-button color="warn" (click)="openDelete(row)" [disabled]="busy()">
                          <span class="material-symbols-outlined">delete</span>
                        </button>
                      }
                    </div>
                  </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
              </table>
            </div>
          }
        </div>

        <div footer>
          <span class="footer-count">{{ totalElements() }} template(s)</span>
          <div class="pagination">
            <button mat-icon-button [disabled]="page() === 0" (click)="prevPage()">
              <span class="material-symbols-outlined">chevron_left</span>
            </button>
            <span>Page {{ page() + 1 }} / {{ totalPages() }}</span>
            <button mat-icon-button [disabled]="page() + 1 >= totalPages()" (click)="nextPage()">
              <span class="material-symbols-outlined">chevron_right</span>
            </button>
          </div>
        </div>
      </tch-admin-crud-shell>
    </tch-admin-page-shell>
  `,
  styles: [
    `
      .table-container { overflow-x: auto; }
      table { width: 100%; }
      .action-row { display: flex; gap: 0.5rem; flex-wrap: wrap; align-items: center; }
      .star-icon { color: var(--tch-color-primary); font-size: 1.25rem; }
      .footer-count { font-size: 0.875rem; color: var(--tch-color-on-surface-variant); }
      .pagination { display: flex; align-items: center; gap: 0.5rem; font-size: 0.875rem; }
    `,
  ],
})
export class PlatformOpsPageModelsPage implements OnInit {
  private readonly api = inject(PlatformPageModelsApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['code', 'logicalId', 'scope', 'name', 'level', 'isDefault', 'actions'];
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly templates = signal<PageModelTemplateView[]>([]);
  readonly search = signal('');
  readonly scopeFilter = signal('');
  readonly page = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);

  levelTone(level: string): AdminStatusTone {
    return level === 'TENANT' ? 'info' : 'neutral';
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listTemplates({ q: this.search() || undefined, scope: this.scopeFilter() || undefined, page: this.page(), size: 20 }).subscribe({
      next: page => {
        this.templates.set(page.content);
        this.totalElements.set(page.totalElements);
        this.totalPages.set(page.totalPages || 1);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  onSearch(v: string): void { this.search.set(v); this.page.set(0); this.load(); }
  onScope(event: Event): void { this.scopeFilter.set((event.target as HTMLInputElement).value); this.page.set(0); this.load(); }
  prevPage(): void { this.page.set(this.page() - 1); this.load(); }
  nextPage(): void { this.page.set(this.page() + 1); this.load(); }

  setDefault(template: PageModelTemplateView): void {
    this.busy.set(true);
    this.api.setDefault(template.id.value).subscribe({
      next: () => { this.busy.set(false); this.snackBar.open('Template défini comme défaut.', 'OK', { duration: 4000 }); this.load(); },
      error: (err: unknown) => {
        this.busy.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.snackBar.open(pd?.title ?? 'Erreur.', 'OK', { duration: 5000 });
      },
    });
  }

  duplicate(template: PageModelTemplateView): void {
    this.busy.set(true);
    this.api.duplicate(template.id.value).subscribe({
      next: () => { this.busy.set(false); this.snackBar.open('Template dupliqué.', 'OK', { duration: 4000 }); this.load(); },
      error: (err: unknown) => {
        this.busy.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.snackBar.open(pd?.title ?? 'Erreur lors de la duplication.', 'OK', { duration: 5000 });
      },
    });
  }

  openDelete(template: PageModelTemplateView): void {
    const ref = this.dialog.open(DeletePageModelDialog, { data: template, width: '400px' });
    ref.afterClosed().subscribe((confirmed: boolean) => {
      if (!confirmed) return;
      this.api.deleteTemplate(template.id.value).subscribe({
        next: () => { this.snackBar.open('Template supprimé.', 'OK', { duration: 4000 }); this.load(); },
        error: (err: unknown) => {
          const pd = (err as { error?: { title?: string } })?.error;
          this.snackBar.open(pd?.title ?? 'Erreur lors de la suppression.', 'OK', { duration: 5000 });
        },
      });
    });
  }
}
