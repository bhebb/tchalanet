import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminCrudShellComponent } from '../../../../shared/admin-ui/admin-crud-shell.component';
import { AdminDataToolbarComponent } from '../../../../shared/admin-ui/admin-data-toolbar.component';
import { AdminEmptyStateComponent } from '../../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { AdminStatusPillComponent, AdminStatusTone } from '../../../../shared/admin-ui/admin-status-pill.component';
import { PageModelTemplateView, PlatformPageModelsApi } from '../../data-access/platform-pagemodels-api.service';
import { DeletePageModelTemplateDialog } from '../../components/dialogs/delete-page-model-template.dialog';

@Component({
  selector: 'tch-platform-catalog-page-model-templates-page',
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
  templateUrl: './platform-catalog-page-model-templates.page.html',
  styleUrls: ['./platform-catalog-page-model-templates.page.scss'],
})
export class PlatformCatalogPageModelTemplatesPage implements OnInit {
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

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listTemplates({ q: this.search() || undefined, scope: this.scopeFilter() || undefined, page: this.page(), size: 20 }).subscribe({
      next: p => {
        this.templates.set(p.items);
        this.totalElements.set(p.totalElements);
        this.totalPages.set(p.totalPages || 1);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur de chargement.');
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
    this.api.setDefault(template.id).subscribe({
      next: () => { this.busy.set(false); this.snackBar.open('Template défini comme défaut.', 'OK', { duration: 4000 }); this.load(); },
      error: (err: unknown) => {
        this.busy.set(false);
        this.snackBar.open((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.', 'OK', { duration: 5000 });
      },
    });
  }

  duplicate(template: PageModelTemplateView): void {
    this.busy.set(true);
    this.api.duplicate(template.id).subscribe({
      next: () => { this.busy.set(false); this.snackBar.open('Template dupliqué.', 'OK', { duration: 4000 }); this.load(); },
      error: (err: unknown) => {
        this.busy.set(false);
        this.snackBar.open((err as { error?: { title?: string } })?.error?.title ?? 'Erreur lors de la duplication.', 'OK', { duration: 5000 });
      },
    });
  }

  openDelete(template: PageModelTemplateView): void {
    const ref = this.dialog.open(DeletePageModelTemplateDialog, { data: template, width: '400px' });
    ref.afterClosed().subscribe((confirmed: boolean) => {
      if (!confirmed) return;
      this.api.deleteTemplate(template.id).subscribe({
        next: () => { this.snackBar.open('Template supprimé.', 'OK', { duration: 4000 }); this.load(); },
        error: (err: unknown) => {
          this.snackBar.open((err as { error?: { title?: string } })?.error?.title ?? 'Erreur lors de la suppression.', 'OK', { duration: 5000 });
        },
      });
    });
  }
}
