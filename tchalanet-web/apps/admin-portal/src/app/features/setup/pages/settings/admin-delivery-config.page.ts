import {
  ChangeDetectionStrategy,
  Component,
  effect,
  inject,
  untracked,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { mapHttpErrorToProblemDetail, webAppErrorsFromProblemDetailFields } from '@tch/api';
import { RuntimeSettingsStore } from '@tch/shared-config';
import { TchFieldError, TchNotice } from '@tch/ui/components';
import {
  applyServerFieldErrors,
  clearServerFieldErrors,
  withResolvedErrorCopies,
} from '@tch/web/errors';
import {
  TchAsyncReadyDirective,
  TchAsyncViewComponent,
  resourceErrorVm,
  tchMutation,
} from '@tch/web/async';
import { AdminPageShellComponent, AdminSectionCardComponent } from '@tch/ui/console';
import {
  TenantParametersApiService,
  TenantInternalConfig,
} from '../../data-access/tenant-parameters-api.service';

const DEFAULT_TENANT_CURRENCY = 'HTG';

const PAID_BY = [
  { code: 'BUYER', labelKey: 'admin.settings.config.communication.paidBy.buyer' },
  { code: 'TENANT', labelKey: 'admin.settings.config.communication.paidBy.tenant' },
  { code: 'SELLER', labelKey: 'admin.settings.config.communication.paidBy.seller' },
] as const;

const COMMUNICATION_FIELD_TARGETS: Record<string, string> = {
  'communication.buyerTicketDelivery.sms.enabled': 'smsEnabled',
  'communication.buyerTicketDelivery.sms.amount': 'smsAmount',
  'communication.buyerTicketDelivery.sms.currency': 'smsCurrency',
  'communication.buyerTicketDelivery.sms.paidBy': 'smsPaidBy',
  'communication.buyerTicketDelivery.whatsapp.enabled': 'whatsappEnabled',
  'communication.buyerTicketDelivery.whatsapp.amount': 'whatsappAmount',
  'communication.buyerTicketDelivery.whatsapp.currency': 'whatsappCurrency',
  'communication.buyerTicketDelivery.whatsapp.paidBy': 'whatsappPaidBy',
  'communication.buyerTicketDelivery.email.enabled': 'emailEnabled',
  'communication.buyerTicketDelivery.email.amount': 'emailAmount',
  'communication.buyerTicketDelivery.email.currency': 'emailCurrency',
  'communication.buyerTicketDelivery.email.paidBy': 'emailPaidBy',
};

@Component({
  selector: 'tch-admin-delivery-config-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    TranslatePipe,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchAsyncViewComponent,
    TchAsyncReadyDirective,
    TchFieldError,
    TchNotice,
  ],
  templateUrl: './admin-delivery-config.page.html',
  styleUrls: ['./admin-delivery-config.page.scss'],
})
export class AdminDeliveryConfigPage {
  private readonly api = inject(TenantParametersApiService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly runtimeSettings = inject(RuntimeSettingsStore);
  private readonly translate = inject(TranslateService);

  readonly fromSetup = this.route.snapshot.queryParamMap.get('from') === 'setup';
  readonly backRoute = this.fromSetup ? '/app/admin/setup' : '/app/admin/company/settings';
  readonly backLabel = this.fromSetup ? 'admin.setup.backToSetup' : 'admin.settings.title';

  readonly currencies = this.runtimeSettings.tenantCurrencyOptions;
  readonly paidByOptions = PAID_BY;

  readonly communicationForm = this.fb.group({
    smsEnabled: new FormControl<boolean>(false, { nonNullable: true }),
    smsAmount: new FormControl<number | null>(5, { validators: [Validators.min(0)] }),
    smsCurrency: new FormControl<string>(DEFAULT_TENANT_CURRENCY, { nonNullable: true }),
    smsPaidBy: new FormControl<string>('BUYER', { nonNullable: true }),
    whatsappEnabled: new FormControl<boolean>(false, { nonNullable: true }),
    whatsappAmount: new FormControl<number | null>(5, { validators: [Validators.min(0)] }),
    whatsappCurrency: new FormControl<string>(DEFAULT_TENANT_CURRENCY, { nonNullable: true }),
    whatsappPaidBy: new FormControl<string>('BUYER', { nonNullable: true }),
    emailEnabled: new FormControl<boolean>(false, { nonNullable: true }),
    emailAmount: new FormControl<number | null>(0, { validators: [Validators.min(0)] }),
    emailCurrency: new FormControl<string>(DEFAULT_TENANT_CURRENCY, { nonNullable: true }),
    emailPaidBy: new FormControl<string>('TENANT', { nonNullable: true }),
  });

  readonly smsEnabled = toSignal(this.communicationForm.controls.smsEnabled.valueChanges, { initialValue: false });
  readonly whatsappEnabled = toSignal(this.communicationForm.controls.whatsappEnabled.valueChanges, { initialValue: false });
  readonly emailEnabled = toSignal(this.communicationForm.controls.emailEnabled.valueChanges, { initialValue: false });

  readonly config = this.api.tenantConfigResource();
  readonly configError = resourceErrorVm(this.config, 'admin.setup.config');
  readonly configIsEmpty = () => false;
  readonly readiness = this.api.readinessResource();

  private lastConfig: TenantInternalConfig = {};

  readonly saveCommunication = tchMutation<
    NonNullable<TenantInternalConfig['communication']>,
    void
  >({
    run: communication =>
      this.api.updateSettingsSection('communication', communication, {
        suppressShellFeedback: true,
      }),
    source: 'admin.setup.communication',
    onSuccess: (_result, input) => {
      this.rememberConfig({ ...this.lastConfig, communication: input });
      this.communicationForm.markAsPristine();
      this.config.reload();
    },
    onError: err => this.applyFieldErrors(err),
  });

  constructor() {
    effect(() => {
      if (!this.config.hasValue()) return;
      const cfg = this.config.value() ?? {};
      untracked(() => {
        this.rememberConfig(cfg);
        if (!this.communicationForm.dirty) this.patchForm(cfg);
      });
    });
  }

  sectionReadiness(section: string) {
    return this.readiness.value()?.sections.find(item => item.section === section) ?? null;
  }

  submitCommunication(): void {
    clearServerFieldErrors(this.communicationForm);
    this.saveCommunication.clearFeedback();
    if (this.communicationForm.invalid) {
      this.communicationForm.markAllAsTouched();
      return;
    }
    const v = this.communicationForm.getRawValue();
    this.saveCommunication.execute({
      ...this.lastConfig.communication,
      buyerTicketDelivery: {
        ...this.lastConfig.communication?.buyerTicketDelivery,
        sms: this.channel(v.smsEnabled, v.smsAmount, v.smsCurrency, v.smsPaidBy),
        whatsapp: this.channel(v.whatsappEnabled, v.whatsappAmount, v.whatsappCurrency, v.whatsappPaidBy),
        email: this.channel(v.emailEnabled, v.emailAmount, v.emailCurrency, v.emailPaidBy),
      },
    });
  }

  private patchForm(cfg: TenantInternalConfig): void {
    const delivery = cfg.communication?.buyerTicketDelivery;
    if (!delivery) return;
    this.communicationForm.patchValue({
      smsEnabled: delivery.sms?.enabled ?? false,
      smsAmount: delivery.sms?.amount ?? null,
      smsCurrency: delivery.sms?.currency ?? DEFAULT_TENANT_CURRENCY,
      smsPaidBy: delivery.sms?.paidBy ?? 'BUYER',
      whatsappEnabled: delivery.whatsapp?.enabled ?? false,
      whatsappAmount: delivery.whatsapp?.amount ?? null,
      whatsappCurrency: delivery.whatsapp?.currency ?? DEFAULT_TENANT_CURRENCY,
      whatsappPaidBy: delivery.whatsapp?.paidBy ?? 'BUYER',
      emailEnabled: delivery.email?.enabled ?? false,
      emailAmount: delivery.email?.amount ?? null,
      emailCurrency: delivery.email?.currency ?? DEFAULT_TENANT_CURRENCY,
      emailPaidBy: delivery.email?.paidBy ?? 'TENANT',
    });
  }

  private channel(enabled: boolean, amount: number | null, currency: string, paidBy: string) {
    return enabled
      ? { enabled, amount: amount ?? 0, currency: currency || DEFAULT_TENANT_CURRENCY, paidBy: paidBy || 'BUYER' }
      : { enabled };
  }

  private rememberConfig(config: TenantInternalConfig): void {
    this.lastConfig = config;
    this.runtimeSettings.applyTenantDashboardSettings({
      supportedLanguages: config.locale?.supportedLanguages,
      fallbackLanguage: config.locale?.fallbackLanguage,
      settings: config,
    });
  }

  private applyFieldErrors(err: unknown): boolean {
    const problem = mapHttpErrorToProblemDetail(err);
    const fieldErrors = withResolvedErrorCopies(
      webAppErrorsFromProblemDetailFields(problem, 'admin.setup.communication'),
      key => this.translate.instant(key),
    );
    const remaining = applyServerFieldErrors(this.communicationForm, fieldErrors, COMMUNICATION_FIELD_TARGETS);
    return fieldErrors.length > 0 && remaining.length === 0;
  }
}
