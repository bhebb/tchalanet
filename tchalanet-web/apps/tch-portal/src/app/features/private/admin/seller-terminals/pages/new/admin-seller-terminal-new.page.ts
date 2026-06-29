import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail, webAppErrorsFromProblemDetailFields } from '@tch/api';

import { TchErrorPanel, TchFieldError } from '@tch/ui/components';
import {
  applyServerFieldErrors,
  clearServerFieldErrors,
  ErrorViewModel,
  toErrorViewModel,
  withResolvedErrorCopies,
} from '@tch/web/errors';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../../shared/admin-ui/admin-section-card.component';
import {
  AddressRequest,
  CreateSellerTerminalResult,
  SellerTerminalApi,
} from '../../../seller-terminal-api.service';
import { SellerTerminalSuccessCardComponent } from '../../components/seller-terminal-success-card/seller-terminal-success-card.component';
import { SELLER_TERMINAL_CREATE_FIELD_TARGETS } from '../../seller-terminal-error-targets';

function generateTerminalCode(): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  const suffix = Array.from(
    { length: 6 },
    () => chars[Math.floor(Math.random() * chars.length)],
  ).join('');
  return `TCH-${suffix}`;
}

function pinMatchValidator(group: AbstractControl): ValidationErrors | null {
  const pin = group.get('initialPin')?.value as string;
  const confirm = group.get('confirmPin')?.value as string;
  if (pin && confirm && pin !== confirm) {
    return { pinMismatch: true };
  }
  return null;
}

@Component({
  selector: 'tch-admin-seller-terminal-new-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    SellerTerminalSuccessCardComponent,
    TchErrorPanel,
    TchFieldError,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSlideToggleModule,
    MatTooltipModule,
  ],
  templateUrl: './admin-seller-terminal-new.page.html',
  styleUrls: ['./admin-seller-terminal-new.page.scss'],
})
export class AdminSellerTerminalNewPage implements OnInit {
  private readonly api = inject(SellerTerminalApi);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  private readonly fallbackCommissionRate = 15;

  readonly saving = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly successResult = signal<CreateSellerTerminalResult | null>(null);
  readonly tenantDefaultCommissionRate = signal<number | null>(null);
  readonly showPin = signal(false);
  readonly showConfirmPin = signal(false);

  readonly form = this.fb.nonNullable.group(
    {
      terminalCode: [generateTerminalCode(), [Validators.required]],
      displayName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(64)]],
      firstName: [''],
      lastName: [''],
      email: ['', [Validators.email, Validators.maxLength(254)]],
      phoneNumber: [''],
      initialPin: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
      confirmPin: ['', [Validators.required]],
      commissionRate: [this.fallbackCommissionRate, [Validators.required, Validators.min(0), Validators.max(100)]],
      active: [true],
      address: this.fb.group({
        line1: ['', [Validators.maxLength(200)]],
        line2: ['', [Validators.maxLength(200)]],
        city: ['', [Validators.maxLength(100)]],
        region: ['', [Validators.maxLength(100)]],
        country: ['HT', [Validators.maxLength(2)]],
        postalCode: ['', [Validators.maxLength(20)]],
      }),
    },
    { validators: pinMatchValidator },
  );

  ngOnInit(): void {
    this.api.getCommissionOverview().subscribe({
      next: overview => {
        const rate = overview.tenantDefaultRate ?? this.fallbackCommissionRate;
        this.tenantDefaultCommissionRate.set(overview.tenantDefaultRate);
        this.form.controls.commissionRate.setValue(rate);
      },
      error: () => {
        this.tenantDefaultCommissionRate.set(null);
      },
    });
  }

  regenerateCode(): void {
    this.form.controls.terminalCode.setValue(generateTerminalCode());
  }

  toggleShowPin(): void {
    this.showPin.update(v => !v);
  }

  toggleShowConfirmPin(): void {
    this.showConfirmPin.update(v => !v);
  }

  onSubmit(): void {
    if (this.saving()) return;
    clearServerFieldErrors(this.form);
    this.error.set(null);
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    const raw = this.form.getRawValue();
    this.saving.set(true);

    const addr = raw.address;
    const addressPayload: AddressRequest | null =
      addr?.line1 || addr?.city
        ? {
            line1: addr.line1 ?? '',
            line2: addr.line2 || null,
            city: addr.city ?? '',
            region: addr.region || null,
            country: addr.country || 'HT',
            postalCode: addr.postalCode || null,
          }
        : null;

    this.api
      .createFull({
        terminalCode: raw.terminalCode,
        displayName: raw.displayName,
        firstName: raw.firstName || null,
        lastName: raw.lastName || null,
        email: raw.email || null,
        phoneNumber: raw.phoneNumber || null,
        commissionRate: raw.commissionRate,
        initialPin: raw.initialPin,
        active: raw.active,
        address: addressPayload,
      }, { suppressShellFeedback: true })
      .subscribe({
        next: result => {
          this.successResult.set(result);
          this.saving.set(false);
        },
        error: (err: unknown) => {
          this.handleCreateError(err);
          this.saving.set(false);
        },
      });
  }

  goBackToList(): void {
    void this.router.navigate(['/app/admin/seller-terminals']);
  }

  onOpenPos(result: CreateSellerTerminalResult): void {
    void this.router.navigate(['/app/admin/seller-terminals', result.sellerTerminalId, 'pos']);
  }

  onCreateAnother(): void {
    this.successResult.set(null);
    this.error.set(null);
    const rate = this.tenantDefaultCommissionRate() ?? this.fallbackCommissionRate;
    clearServerFieldErrors(this.form);
    this.form.reset({
      terminalCode: generateTerminalCode(),
      displayName: '',
      firstName: '',
      lastName: '',
      email: '',
      phoneNumber: '',
      initialPin: '',
      confirmPin: '',
      commissionRate: rate,
      active: true,
      address: { line1: '', line2: '', city: '', region: '', country: 'HT', postalCode: '' },
    });
  }

  serverFieldMessage(control: AbstractControl | null): string {
    const server = control?.errors?.['server'];
    return typeof server === 'object' &&
      server !== null &&
      'message' in server &&
      typeof (server as { message?: unknown }).message === 'string'
      ? (server as { message: string }).message
      : '';
  }

  private handleCreateError(err: unknown): void {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const fieldErrors = withResolvedErrorCopies(
        webAppErrorsFromProblemDetailFields(problem, 'admin.sellerTerminal.create'),
        key => this.translate.instant(key),
      );
      const remaining = applyServerFieldErrors(this.form, fieldErrors, SELLER_TERMINAL_CREATE_FIELD_TARGETS);

      if (fieldErrors.length && !remaining.length) {
        this.error.set(null);
        return;
      }

      const normalized = webAppErrorFromProblemDetail(problem, 'admin.sellerTerminal.create', 'page');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      this.error.set(toErrorViewModel(normalized, copy));
      return;
    }

    this.error.set({
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    });
  }
}
