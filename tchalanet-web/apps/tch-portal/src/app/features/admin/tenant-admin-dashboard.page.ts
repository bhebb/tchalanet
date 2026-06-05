import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { CreatedUserView, CreateUserRequest, TenantAdminApi } from './tenant-admin-api.service';

/**
 * Minimal TENANT_ADMIN dashboard: onboard a seller (role=CASHIER) into the current tenant.
 * The backend resolves the tenant from request context, so no tenant override is exposed.
 * `outletId` is required (backend rejects CASHIER without it).
 */
@Component({
  selector: 'tch-tenant-admin-dashboard-page',
  imports: [ReactiveFormsModule, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="dash">
      <h1>{{ 'admin.seller.title' | translate }}</h1>

      <form class="dash__form" [formGroup]="form" (ngSubmit)="submit()">
        <label>
          <span>{{ 'admin.seller.email' | translate }}</span>
          <input type="email" formControlName="email" autocomplete="off" />
        </label>
        <label>
          <span>{{ 'admin.seller.firstName' | translate }}</span>
          <input type="text" formControlName="firstName" autocomplete="off" />
        </label>
        <label>
          <span>{{ 'admin.seller.lastName' | translate }}</span>
          <input type="text" formControlName="lastName" autocomplete="off" />
        </label>
        <label>
          <span>{{ 'admin.seller.phone' | translate }}</span>
          <input type="tel" formControlName="phone" autocomplete="off" />
        </label>
        <label>
          <span>{{ 'admin.seller.outletId' | translate }}</span>
          <input type="text" formControlName="outletId" autocomplete="off" />
        </label>
        <label>
          <span>{{ 'admin.seller.terminalId' | translate }}</span>
          <input type="text" formControlName="terminalId" autocomplete="off" />
        </label>

        <button type="submit" [disabled]="form.invalid || busy()">
          {{ 'admin.seller.submit' | translate }}
        </button>
      </form>

      @if (result(); as r) {
        <div class="dash__panel dash__panel--ok" role="status">
          {{ 'admin.seller.success' | translate }} {{ r.email ?? r.id }}
        </div>
      }

      @if (error()) {
        <div class="dash__panel dash__panel--error" role="alert">
          {{ 'admin.seller.error' | translate }}
        </div>
      }
    </section>
  `,
  styles: [
    `
      .dash {
        display: grid;
        gap: 1.5rem;
        max-width: 560px;
        padding: 2rem;
        color: var(--tch-color-foreground, var(--mat-sys-on-surface));
      }
      .dash__form {
        display: grid;
        gap: 1rem;
      }
      label {
        display: grid;
        gap: 0.35rem;
        font-weight: 600;
      }
      input {
        padding: 0.5rem 0.65rem;
        border-radius: var(--tch-radius-control, 8px);
        border: 1px solid var(--tch-color-outline, var(--mat-sys-outline-variant));
        background: var(--tch-color-surface, var(--mat-sys-surface));
        color: inherit;
      }
      button {
        justify-self: start;
        padding: 0.6rem 1.1rem;
        border-radius: var(--tch-radius-control, 8px);
        border: 1px solid var(--tch-color-primary, var(--mat-sys-primary));
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-primary-contrast, var(--mat-sys-on-primary));
        font-weight: 600;
        cursor: pointer;
      }
      button:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
      .dash__panel {
        padding: 1rem 1.25rem;
        border-radius: var(--tch-radius-control, 8px);
        border: 1px solid var(--tch-color-outline, var(--mat-sys-outline-variant));
      }
      .dash__panel--ok {
        border-color: var(--tch-color-primary, var(--mat-sys-primary));
      }
      .dash__panel--error {
        border-color: var(--tch-color-error, var(--mat-sys-error));
        color: var(--tch-color-error, var(--mat-sys-error));
      }
    `,
  ],
})
export class TenantAdminDashboardPage {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(TenantAdminApi);

  readonly busy = signal(false);
  readonly error = signal(false);
  readonly result = signal<CreatedUserView | null>(null);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    firstName: [''],
    lastName: [''],
    phone: [''],
    outletId: ['', Validators.required],
    terminalId: [''],
  });

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.busy.set(true);
    this.error.set(false);
    this.result.set(null);

    this.api.createSeller(this.payload()).subscribe({
      next: view => {
        this.result.set(view);
        this.busy.set(false);
      },
      error: () => {
        this.busy.set(false);
        this.error.set(true);
      },
    });
  }

  private payload(): CreateUserRequest {
    const value = this.form.getRawValue();
    return {
      email: value.email,
      firstName: value.firstName || undefined,
      lastName: value.lastName || undefined,
      phone: value.phone || undefined,
      role: 'CASHIER',
      outletId: value.outletId,
      terminalId: value.terminalId || undefined,
    };
  }
}
