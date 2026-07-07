import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminDataToolbarComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminStatusPillComponent, AdminStatusTone } from '@tch/ui/console';
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
  private readonly fb = inject(FormBuilder);
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
  readonly hasNext = signal(false);
  readonly hasPrevious = signal(false);
  readonly creating = signal(false);
  readonly selectedTemplate = signal<PageModelTemplateView | null>(null);
  readonly createForm = this.fb.nonNullable.group({
    code: ['', Validators.required],
    logicalId: ['', Validators.required],
    scope: ['private', Validators.required],
    slug: ['dashboard', Validators.required],
    name: ['', Validators.required],
    label: ['', Validators.required],
    description: [''],
    schema: ['{}'],
    model: ['{}'],
  });
  readonly configForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    label: ['', Validators.required],
    description: [''],
    schema: ['{}'],
    model: ['{}'],
  });

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
        this.page.set(p.page);
        this.totalPages.set(p.totalPages || 1);
        this.hasNext.set(p.hasNext ?? false);
        this.hasPrevious.set(p.hasPrevious ?? false);
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
  prevPage(): void { if (this.hasPrevious()) { this.page.set(this.page() - 1); this.load(); } }
  nextPage(): void { if (this.hasNext()) { this.page.set(this.page() + 1); this.load(); } }

  openCreate(): void {
    this.creating.set(true);
    this.selectedTemplate.set(null);
    this.createForm.reset({
      code: '',
      logicalId: '',
      scope: 'private',
      slug: 'dashboard',
      name: '',
      label: '',
      description: '',
      schema: '{}',
      model: '{}',
    });
  }

  closeCreate(): void {
    this.creating.set(false);
  }

  saveCreate(): void {
    if (this.createForm.invalid || this.busy()) return;

    const v = this.createForm.getRawValue();
    const schema = this.parseJson(v.schema, 'Schema JSON invalide.');
    if (schema === undefined) return;
    const model = v.model.trim() === '{}'
      ? defaultTemplateModel(v.logicalId, v.scope, v.slug)
      : this.parseJson(v.model, 'Model JSON invalide.');
    if (model === undefined) return;

    const template = minimalTemplate({
      code: v.code,
      logicalId: v.logicalId,
      scope: v.scope,
      slug: v.slug,
      name: v.name,
      label: v.label,
      description: v.description,
      schema,
      model,
    });

    this.busy.set(true);
    this.api.createTemplate(template).subscribe({
      next: created => {
        this.busy.set(false);
        this.creating.set(false);
        this.snackBar.open('Template créé.', 'OK', { duration: 4000 });
        this.openConfig(created);
        this.load();
      },
      error: err => this.handleActionError(err, 'Erreur lors de la création.'),
    });
  }

  openConfig(template: PageModelTemplateView): void {
    this.selectedTemplate.set(template);
    this.configForm.reset({
      name: template.name,
      label: template.label,
      description: template.description ?? '',
      schema: this.formatJson(template.schema ?? {}),
      model: this.formatJson(template.model ?? {}),
    });
  }

  closeConfig(): void {
    this.selectedTemplate.set(null);
  }

  saveConfig(): void {
    const template = this.selectedTemplate();
    if (!template || this.configForm.invalid || this.busy()) return;

    const v = this.configForm.getRawValue();
    const schema = this.parseJson(v.schema, 'Schema JSON invalide.');
    if (schema === undefined) return;
    const model = this.parseJson(v.model, 'Model JSON invalide.');
    if (model === undefined) return;

    this.busy.set(true);
    this.api.updateTemplate(template.id, {
      ...template,
      name: v.name.trim(),
      label: v.label.trim(),
      description: v.description?.trim() || undefined,
      schema,
      model,
    }).subscribe({
      next: updated => {
        this.busy.set(false);
        this.snackBar.open('Template mis à jour.', 'OK', { duration: 4000 });
        this.openConfig(updated);
        this.load();
      },
      error: err => this.handleActionError(err, 'Erreur lors de la mise à jour.'),
    });
  }

  setDefault(template: PageModelTemplateView): void {
    this.busy.set(true);
    this.api.setDefault(template.id).subscribe({
      next: () => { this.busy.set(false); this.snackBar.open('Template défini comme défaut.', 'OK', { duration: 4000 }); this.load(); },
      error: (err: unknown) => this.handleActionError(err, 'Erreur.'),
    });
  }

  duplicate(template: PageModelTemplateView): void {
    this.busy.set(true);
    this.api.duplicate(template.id).subscribe({
      next: () => { this.busy.set(false); this.snackBar.open('Template dupliqué.', 'OK', { duration: 4000 }); this.load(); },
      error: (err: unknown) => this.handleActionError(err, 'Erreur lors de la duplication.'),
    });
  }

  reset(template: PageModelTemplateView): void {
    if (!confirm(`Réinitialiser le template « ${template.name} » ?`)) return;
    this.busy.set(true);
    this.api.reset(template.id).subscribe({
      next: () => { this.busy.set(false); this.snackBar.open('Template réinitialisé.', 'OK', { duration: 4000 }); this.load(); },
      error: (err: unknown) => this.handleActionError(err, 'Erreur lors de la réinitialisation.'),
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

  private handleActionError(err: unknown, fallback: string): void {
    this.busy.set(false);
    this.snackBar.open((err as { error?: { title?: string } })?.error?.title ?? fallback, 'OK', {
      duration: 5000,
    });
  }

  private formatJson(value: unknown): string {
    return JSON.stringify(value ?? {}, null, 2);
  }

  private parseJson(value: string, message: string): unknown | undefined {
    try {
      return value.trim() ? JSON.parse(value) : {};
    } catch {
      this.snackBar.open(message, 'OK', { duration: 5000 });
      return undefined;
    }
  }
}

function minimalTemplate(input: {
  readonly code: string;
  readonly logicalId: string;
  readonly scope: string;
  readonly slug: string;
  readonly name: string;
  readonly label: string;
  readonly description: string;
  readonly schema: unknown;
  readonly model: unknown;
}): PageModelTemplateView {
  const code = input.code.trim();
  const logicalId = input.logicalId.trim();
  const scope = input.scope.trim() || 'private';
  const slug = input.slug.trim();
  const name = input.name.trim();
  return {
    id: '',
    code,
    logicalId,
    scope,
    slug,
    name,
    label: input.label.trim(),
    description: input.description.trim(),
    schema: input.schema ?? {},
    model: input.model ?? defaultTemplateModel(logicalId, scope, slug),
    schemaVersion: 2,
    isDefault: false,
    level: 'GLOBAL',
    tenantId: null,
    createdAt: '',
    updatedAt: '',
  };
}

function defaultTemplateModel(logicalId: string, scope: string, slug: string): unknown {
  return {
    meta: {
      id: logicalId.trim(),
      scope: scope.trim() || 'private',
      slug: slug.trim(),
      schemaVersion: 2,
    },
    content: {
      layout: {
        component: 'GridLayout',
        rows: [],
      },
      widgets: {},
    },
  };
}
