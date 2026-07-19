import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import {
  form,
  max,
  maxLength,
  min,
  minLength,
  pattern,
  required,
  submit,
  validate,
} from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';
import { TranslateService } from '@ngx-translate/core';
import {
  mapHttpErrorToProblemDetail,
  webAppErrorFromProblemDetail,
  webAppErrorsFromProblemDetailFields,
} from '@tch/api';

import { ErrorViewModel, toErrorViewModel, withResolvedErrorCopies } from '@tch/web/errors';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { AdminPageShellComponent } from '@tch/ui/console';
import {
  AddressRequest,
  CreateSellerTerminalResult,
  SellerTerminalApi,
} from '../../data-access/seller-terminal-api.service';
import {
  SELLER_TERMINAL_CREATE_FIELD_TARGETS,
  SellerTerminalServerFieldError,
  sellerTerminalServerFieldError,
} from '../../seller-terminal-error-targets';
import { SellerTerminalCreateFormComponent } from '../../components/seller-terminal-create-form/seller-terminal-create-form.component';
import { SellerTerminalSuccessCardComponent } from '../../components/seller-terminal-success-card/seller-terminal-success-card.component';

export interface SellerTerminalCreateFormModel {
  readonly terminalCode: string;
  readonly displayName: string;
  readonly firstName: string;
  readonly lastName: string;
  readonly email: string;
  readonly phoneNumber: string;
  readonly initialPin: string;
  readonly confirmPin: string;
  readonly commissionRate: number;
  readonly active: boolean;
  readonly address: {
    readonly line1: string;
    readonly line2: string;
    readonly city: string;
    readonly region: string;
    readonly country: string;
    readonly postalCode: string;
  };
}

@Component({
  selector: 'tch-admin-seller-terminal-new-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    AdminPageShellComponent,
    SellerTerminalCreateFormComponent,
    SellerTerminalSuccessCardComponent,
    TranslatePipe,
    MatButtonModule,
  ],
  templateUrl: './admin-seller-terminal-new.page.html',
  styleUrls: ['./admin-seller-terminal-new.page.scss'],
})
export class AdminSellerTerminalNewPage implements OnInit {
  private readonly api = inject(SellerTerminalApi);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);

  private readonly fallbackCommissionRate = 15;

  readonly saving = signal(false);
  readonly codeLoading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly successResult = signal<CreateSellerTerminalResult | null>(null);
  readonly tenantDefaultCommissionRate = signal<number | null>(null);
  readonly showPin = signal(false);
  readonly showConfirmPin = signal(false);
  readonly serverFieldErrors = signal<Readonly<Record<string, SellerTerminalServerFieldError>>>({});

  readonly model = signal<SellerTerminalCreateFormModel>(this.initialFormModel());
  readonly form = form(this.model, path => {
    required(path.terminalCode, {
      message: 'admin.sellerTerminals.new.validation.terminalCodeRequired',
    });
    maxLength(path.terminalCode, 64, {
      message: 'admin.sellerTerminals.new.validation.terminalCodeMax',
    });
    required(path.displayName, {
      message: 'admin.sellerTerminals.new.validation.displayNameRequired',
    });
    minLength(path.displayName, 2, {
      message: 'admin.sellerTerminals.new.validation.displayNameMin',
    });
    maxLength(path.displayName, 64, {
      message: 'admin.sellerTerminals.new.validation.displayNameMax',
    });
    maxLength(path.email, 254, { message: 'admin.sellerTerminals.new.validation.emailMax' });
    pattern(path.email, /^[^@\s]+@[^@\s]+\.[^@\s]+$|^$/, {
      message: 'admin.sellerTerminals.new.validation.email',
    });
    required(path.initialPin, { message: 'admin.sellerTerminals.new.validation.pinRequired' });
    pattern(path.initialPin, /^\d{6}$/, {
      message: 'admin.sellerTerminals.new.validation.pinFormat',
    });
    required(path.confirmPin, {
      message: 'admin.sellerTerminals.new.validation.confirmPinRequired',
    });
    pattern(path.confirmPin, /^\d{6}$/, {
      message: 'admin.sellerTerminals.new.validation.confirmPinFormat',
    });
    required(path.commissionRate, {
      message: 'admin.sellerTerminals.new.validation.commissionRequired',
    });
    min(path.commissionRate, 0, {
      message: 'admin.sellerTerminals.new.validation.commissionRange',
    });
    max(path.commissionRate, 100, {
      message: 'admin.sellerTerminals.new.validation.commissionRange',
    });
    maxLength(path.address.line1, 200, { message: 'admin.sellerTerminals.new.validation.max200' });
    maxLength(path.address.line2, 200, { message: 'admin.sellerTerminals.new.validation.max200' });
    maxLength(path.address.city, 100, { message: 'admin.sellerTerminals.new.validation.max100' });
    maxLength(path.address.region, 100, { message: 'admin.sellerTerminals.new.validation.max100' });
    maxLength(path.address.country, 2, { message: 'admin.sellerTerminals.new.validation.country' });
    maxLength(path.address.postalCode, 20, {
      message: 'admin.sellerTerminals.new.validation.postalCode',
    });

    validate(path.terminalCode, ({ value }) => this.serverError('terminalCode', value()));
    validate(path.displayName, ({ value }) => this.serverError('displayName', value()));
    validate(path.firstName, ({ value }) => this.serverError('firstName', value()));
    validate(path.lastName, ({ value }) => this.serverError('lastName', value()));
    validate(path.email, ({ value }) => this.serverError('email', value()));
    validate(path.phoneNumber, ({ value }) => this.serverError('phoneNumber', value()));
    validate(path.initialPin, ({ value }) => this.serverError('initialPin', value()));
    validate(path.commissionRate, ({ value }) => this.serverError('commissionRate', value()));
    validate(path.address.line1, ({ value }) => this.serverError('address.line1', value()));
    validate(path.address.line2, ({ value }) => this.serverError('address.line2', value()));
    validate(path.address.city, ({ value }) => this.serverError('address.city', value()));
    validate(path.address.region, ({ value }) => this.serverError('address.region', value()));
    validate(path.address.country, ({ value }) => this.serverError('address.country', value()));
    validate(path.address.postalCode, ({ value }) =>
      this.serverError('address.postalCode', value()),
    );
  });
  readonly pinMismatch = computed(() => {
    const value = this.model();
    return (
      this.form.confirmPin().touched() &&
      !!value.initialPin &&
      !!value.confirmPin &&
      value.initialPin !== value.confirmPin
    );
  });

  ngOnInit(): void {
    this.refreshTerminalCode();

    this.api.getCommissionOverview().subscribe({
      next: overview => {
        const rate = overview.tenantDefaultRate ?? this.fallbackCommissionRate;
        this.tenantDefaultCommissionRate.set(overview.tenantDefaultRate);
        this.model.update(value => ({ ...value, commissionRate: rate }));
      },
      error: () => {
        this.tenantDefaultCommissionRate.set(null);
      },
    });
  }

  regenerateCode(): void {
    this.refreshTerminalCode();
  }

  toggleShowPin(): void {
    this.showPin.update(v => !v);
  }

  toggleShowConfirmPin(): void {
    this.showConfirmPin.update(v => !v);
  }

  pinLength(controlName: 'initialPin' | 'confirmPin'): number {
    return this.model()[controlName].length;
  }

  onSubmit(): void {
    if (this.saving()) return;
    this.error.set(null);
    this.serverFieldErrors.set({});
    submit(this.form, async () => {
      const raw = this.model();
      if (raw.initialPin !== raw.confirmPin) return;
      this.saving.set(true);

      const addr = raw.address;
      const addressPayload: AddressRequest | null =
        addr.line1 || addr.city
          ? {
              line1: addr.line1,
              line2: addr.line2 || null,
              city: addr.city,
              region: addr.region || null,
              country: addr.country || 'HT',
              postalCode: addr.postalCode || null,
            }
          : null;

      this.api
        .createFull(
          {
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
          },
          { suppressShellFeedback: true },
        )
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
    });
  }

  goBackToList(): void {
    void this.router.navigate(['/app/admin/seller-terminals']);
  }

  onOpenPos(result: CreateSellerTerminalResult): void {
    void this.router.navigate(['/app/admin/pos/sale', result.sellerTerminalId]);
  }

  onOpenOverrides(result: CreateSellerTerminalResult): void {
    void this.router.navigate([
      '/app/admin/seller-terminals',
      result.sellerTerminalId,
      'overrides',
    ]);
  }

  onCreateAnother(): void {
    this.successResult.set(null);
    this.error.set(null);
    const rate = this.tenantDefaultCommissionRate() ?? this.fallbackCommissionRate;
    this.model.set(this.initialFormModel(rate));
    this.refreshTerminalCode();
  }

  private refreshTerminalCode(): void {
    if (this.codeLoading()) return;
    this.codeLoading.set(true);
    this.api.suggestTerminalCode({ suppressShellFeedback: true }).subscribe({
      next: suggestion => {
        this.model.update(value => ({ ...value, terminalCode: suggestion.terminalCode }));
        this.codeLoading.set(false);
      },
      error: () => {
        this.codeLoading.set(false);
      },
    });
  }

  private handleCreateError(err: unknown): void {
    const problem = mapHttpErrorToProblemDetail(err);
    const fieldErrors = withResolvedErrorCopies(
      webAppErrorsFromProblemDetailFields(problem, 'admin.sellerTerminal.create'),
      key => this.translate.instant(key),
    );

    const serverErrors = Object.fromEntries(
      fieldErrors.flatMap(error => {
        const field = this.mappedField(error.target) ?? this.mappedField(error.field);
        return field && error.message
          ? [[field, { value: this.valueAt(field), message: error.message }]]
          : [];
      }),
    );
    this.serverFieldErrors.set(serverErrors);

    const unconsumed = fieldErrors.filter(
      error => !this.mappedField(error.target) && !this.mappedField(error.field),
    );
    if (fieldErrors.length && unconsumed.length === 0) {
      this.error.set(null);
      return;
    }

    const normalized = webAppErrorFromProblemDetail(problem, 'admin.sellerTerminal.create', 'page');
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    this.error.set(
      toErrorViewModel(normalized, {
        ...copy,
        message: unconsumed[0]?.message ?? copy.message,
      }),
    );
  }

  private serverError(
    field: string,
    value: unknown,
  ): { kind: 'server'; message: string } | undefined {
    return sellerTerminalServerFieldError(this.serverFieldErrors(), field, value);
  }

  private mappedField(target: string | undefined): string | undefined {
    return target
      ? (SELLER_TERMINAL_CREATE_FIELD_TARGETS as Record<string, string>)[target]
      : undefined;
  }

  private valueAt(path: string): unknown {
    return path
      .split('.')
      .reduce<unknown>(
        (value, segment) =>
          value && typeof value === 'object'
            ? (value as Record<string, unknown>)[segment]
            : undefined,
        this.model(),
      );
  }

  private initialFormModel(rate = this.fallbackCommissionRate): SellerTerminalCreateFormModel {
    return {
      terminalCode: 'POS-001',
      displayName: '',
      firstName: '',
      lastName: '',
      email: '',
      phoneNumber: '',
      initialPin: '',
      confirmPin: '',
      commissionRate: rate,
      active: true,
      address: {
        line1: '',
        line2: '',
        city: '',
        region: '',
        country: 'HT',
        postalCode: '',
      },
    };
  }
}
