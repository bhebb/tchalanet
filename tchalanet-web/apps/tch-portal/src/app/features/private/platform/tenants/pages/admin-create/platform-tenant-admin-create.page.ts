import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../../shared/admin-ui/admin-section-card.component';
import { PlatformTenantsApi, TenantSummaryView } from '../../data-access/platform-tenants-api.service';

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
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    TranslatePipe,
  ],
  templateUrl: './platform-tenant-admin-create.page.html',
  styleUrls: ['./platform-tenant-admin-create.page.scss'],
})
export class PlatformTenantAdminCreatePage implements OnInit {
  private readonly api = inject(PlatformTenantsApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly loadingTenant = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly tenant = signal<TenantSummaryView | null>(null);
  readonly pageTitle = computed(() => {
    const tenant = this.tenant();
    return tenant
      ? this.translate.instant('platform.tenants.admin.pageTitleWithTenant', { name: tenant.name })
      : this.translate.instant('platform.tenants.admin.pageTitle');
  });

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    firstName: ['', Validators.required],
    lastName: [''],
    phone: [''],
  });

  private get tenantId(): string | null {
    return this.route.snapshot.paramMap.get('tenantId');
  }

  ngOnInit(): void {
    const tenantId = this.tenantId;
    if (!tenantId) {
      this.error.set(this.localErrorViewModel('platform.tenants.admin.error.create'));
      return;
    }

    this.loadingTenant.set(true);
    this.api.getTenant(tenantId, { suppressShellFeedback: true }).subscribe({
      next: t => {
        this.tenant.set(t);
        this.loadingTenant.set(false);
      },
      error: err => {
        this.error.set(this.errorViewModel(err, 'platform.tenants.adminCreate.tenant'));
        this.loadingTenant.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid || this.loading()) return;

    const v = this.form.getRawValue();
    const tenantId = this.tenantId;
    if (!v.email || !tenantId) return;

    this.loading.set(true);
    this.error.set(null);

    this.api
      .createTenantAdmin(tenantId, {
        email: v.email,
        firstName: v.firstName || null,
        lastName: v.lastName || null,
        phone: v.phone || null,
        role: 'TENANT_ADMIN',
      }, { suppressShellFeedback: true })
      .subscribe({
        next: () => {
          this.loading.set(false);
          void this.router.navigate(['..'], { relativeTo: this.route });
        },
        error: (err: unknown) => {
          this.loading.set(false);
          this.error.set(this.errorViewModel(err, 'platform.tenants.adminCreate.create'));
        },
      });
  }

  private errorViewModel(err: unknown, source: string): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, source, 'page');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return toErrorViewModel(normalized, copy);
    }

    return {
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    };
  }

  private localErrorViewModel(messageKey: string): ErrorViewModel {
    return {
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant(messageKey),
      severity: 'error',
    };
  }
}
