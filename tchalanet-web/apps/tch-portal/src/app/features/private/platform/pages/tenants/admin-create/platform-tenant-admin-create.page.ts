import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../../shared/admin-ui/admin-section-card.component';
import { PlatformTenantsApi, TenantSummaryView } from '../../../platform-tenants-api.service';

@Component({
  selector: 'tch-platform-tenant-admin-create-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  templateUrl: './platform-tenant-admin-create.page.html',
  styleUrls: ['./platform-tenant-admin-create.page.scss'],
})
export class PlatformTenantAdminCreatePage implements OnInit {
  private readonly api = inject(PlatformTenantsApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly loadingTenant = signal(false);
  readonly error = signal<string | null>(null);
  readonly traceId = signal<string | null>(null);
  readonly tenant = signal<TenantSummaryView | null>(null);
  readonly pageTitle = computed(() =>
    this.tenant() ? `Ajouter un admin — ${this.tenant()!.name}` : 'Ajouter un admin',
  );

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    displayName: ['', Validators.required],
    phoneNumber: [''],
    sendInvite: [true],
  });

  private get tenantId(): string {
    return this.route.snapshot.paramMap.get('tenantId')!;
  }

  ngOnInit(): void {
    this.loadingTenant.set(true);
    this.api.getTenant(this.tenantId).subscribe({
      next: t => {
        this.tenant.set(t);
        this.loadingTenant.set(false);
      },
      error: () => this.loadingTenant.set(false),
    });
  }

  submit(): void {
    if (this.form.invalid || this.loading()) return;

    const v = this.form.value;
    this.loading.set(true);
    this.error.set(null);

    this.api
      .createTenantAdmin(this.tenantId, {
        email: v.email!,
        displayName: v.displayName!,
        phoneNumber: v.phoneNumber || null,
        roleCodes: ['TENANT_ADMIN'],
        sendInvite: v.sendInvite ?? true,
      })
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.snackBar.open('Administrateur créé avec succès.', 'OK', { duration: 4000 });
          void this.router.navigate(['..'], { relativeTo: this.route });
        },
        error: (err: unknown) => {
          this.loading.set(false);
          const pd = (err as { error?: { title?: string; errorId?: string; requestId?: string } })
            ?.error;
          this.error.set(pd?.title ?? 'Erreur lors de la création.');
          this.traceId.set(pd?.errorId ?? pd?.requestId ?? null);
        },
      });
  }
}
