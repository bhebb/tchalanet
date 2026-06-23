import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs';

import {
  PlatformTenantsApi,
  TenantSummaryView,
} from '../../tenants/data-access/platform-tenants-api.service';

export interface AssignTenantResult {
  tenantId: string;
  tenantName: string;
}

@Component({
  selector: 'tch-assign-tenant-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatAutocompleteModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './assign-tenant-dialog.component.html',
})
export class AssignTenantDialog implements OnInit {
  private readonly dialogRef = inject(MatDialogRef<AssignTenantDialog, AssignTenantResult>);
  private readonly tenantsApi = inject(PlatformTenantsApi);
  private readonly destroyRef = inject(DestroyRef);

  readonly search = new FormControl('');
  readonly options = signal<TenantSummaryView[]>([]);
  readonly loading = signal(false);
  readonly selected = signal<TenantSummaryView | null>(null);

  ngOnInit(): void {
    this.search.valueChanges.pipe(
      debounceTime(250),
      distinctUntilChanged(),
      switchMap(q => {
        if (typeof q !== 'string') return [];
        this.loading.set(true);
        this.selected.set(null);
        return this.tenantsApi.listTenants({ q, page: 0, size: 10, status: 'ACTIVE' });
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: res => {
        this.options.set(res.items);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  displayFn(tenant: TenantSummaryView | string | null): string {
    if (!tenant || typeof tenant === 'string') return tenant ?? '';
    return tenant.name;
  }

  select(tenant: TenantSummaryView): void {
    this.selected.set(tenant);
  }

  confirm(): void {
    const t = this.selected();
    if (!t) return;
    this.dialogRef.close({ tenantId: t.id, tenantName: t.name });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
