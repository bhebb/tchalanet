import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
  untracked,
} from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
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
  TenantSettingsReadinessSection,
  TenantRecurringHoliday,
} from '../../data-access/tenant-parameters-api.service';

const PAPER_SIZES = ['RECEIPT_58MM', 'RECEIPT_80MM', 'A4'] as const;

const WEEKDAYS = [
  { code: 'MONDAY', label: 'Lundi' },
  { code: 'TUESDAY', label: 'Mardi' },
  { code: 'WEDNESDAY', label: 'Mercredi' },
  { code: 'THURSDAY', label: 'Jeudi' },
  { code: 'FRIDAY', label: 'Vendredi' },
  { code: 'SATURDAY', label: 'Samedi' },
  { code: 'SUNDAY', label: 'Dimanche' },
] as const;

const PAID_BY = [
  { code: 'BUYER', label: 'Client' },
  { code: 'TENANT', label: 'Tenant' },
  { code: 'SELLER', label: 'Seller-terminal' },
] as const;

const LOCALE_FIELD_TARGETS: Record<string, string> = {
  'locale.supportedLanguages': 'supportedLanguages',
  'admin.setup.locale.supportedLanguages': 'supportedLanguages',
  'tenant.locale.supportedLanguages': 'supportedLanguages',
  'locale.fallbackLanguage': 'fallbackLanguage',
  'admin.setup.locale.fallbackLanguage': 'fallbackLanguage',
  'tenant.locale.fallbackLanguage': 'fallbackLanguage',
};

const RECEIPT_FIELD_TARGETS: Record<string, string> = {
  'document.receipt.headerMessage': 'headerMessage',
  'admin.setup.receipt.headerMessage': 'headerMessage',
  'receipt.headerMessage': 'headerMessage',
  'document.receipt.footerMessage': 'footerMessage',
  'admin.setup.receipt.footerMessage': 'footerMessage',
  'receipt.footerMessage': 'footerMessage',
  'document.receipt.defaultPaperSize': 'defaultPaperSize',
  'admin.setup.receipt.defaultPaperSize': 'defaultPaperSize',
  'receipt.defaultPaperSize': 'defaultPaperSize',
};

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

const CALENDAR_FIELD_TARGETS: Record<string, string> = {
  'rules.businessCalendar.defaultOpen': 'defaultOpen',
  'admin.setup.calendar.defaultOpen': 'defaultOpen',
  'calendar.defaultOpen': 'defaultOpen',
  'rules.businessCalendar.closedWeekdays': 'closedWeekdays',
  'admin.setup.calendar.closedWeekdays': 'closedWeekdays',
  'calendar.closedWeekdays': 'closedWeekdays',
  'rules.businessCalendar.holidays': 'holidayTemplateKeys',
  'admin.setup.calendar.holidays': 'holidayTemplateKeys',
  'calendar.holidays': 'holidayTemplateKeys',
};

@Component({
  selector: 'tch-admin-config-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchAsyncViewComponent,
    TchAsyncReadyDirective,
    TchFieldError,
    TchNotice,
    TranslatePipe,
    RouterLink,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTabsModule,
  ],
  templateUrl: './admin-config.page.html',
  styleUrls: ['./admin-config.page.scss'],
})
export class AdminConfigPage {
  private readonly api = inject(TenantParametersApiService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly runtimeSettings = inject(RuntimeSettingsStore);
  private readonly translate = inject(TranslateService);

  readonly languages = this.runtimeSettings.tenantLanguageOptions;
  readonly currencies = this.runtimeSettings.tenantCurrencyOptions;
  readonly defaultCurrency = computed(() => this.runtimeSettings.tenantCurrency());
  readonly paperSizes = PAPER_SIZES;
  readonly weekdays = WEEKDAYS;
  readonly paidByOptions = PAID_BY;

  readonly config = this.api.tenantConfigResource();
  readonly holidayTemplates = this.api.holidayTemplatesResource();
  readonly readiness = this.api.readinessResource();
  readonly configError = resourceErrorVm(this.config, 'admin.setup.config');
  readonly configIsEmpty = () => false;
  readonly fromSetup = this.route.snapshot.queryParamMap.get('from') === 'setup';
  readonly backRoute = this.fromSetup ? '/app/admin/setup' : '/app/admin/company/settings';
  readonly backLabel = this.fromSetup ? 'admin.setup.backToSetup' : 'admin.settings.title';
  readonly customHolidays = signal<TenantRecurringHoliday[]>([]);
  readonly selectedTabIndex = signal(0);
  private readonly requestedFragment = toSignal(this.route.fragment, { initialValue: null });

  readonly localeForm = this.fb.group({
    supportedLanguages: new FormControl<string[]>(
      [...this.runtimeSettings.tenantSupportedLanguages()],
      { nonNullable: true, validators: [Validators.required] },
    ),
    fallbackLanguage: new FormControl<string>(
      this.runtimeSettings.tenantSupportedLanguages()[0] ?? 'ht',
      { nonNullable: true },
    ),
  });

  readonly receiptForm = this.fb.group({
    enabled: new FormControl<boolean>(true, { nonNullable: true }),
    headerMessage: new FormControl<string>('', { nonNullable: true }),
    footerMessage: new FormControl<string>('', { nonNullable: true }),
    defaultPaperSize: new FormControl<string>('RECEIPT_80MM', { nonNullable: true }),
    showQrCode: new FormControl<boolean>(true, { nonNullable: true }),
  });

  readonly communicationForm = this.fb.group({
    smsEnabled: new FormControl<boolean>(true, { nonNullable: true }),
    smsAmount: new FormControl<number | null>(5, { validators: [Validators.min(0)] }),
    smsCurrency: new FormControl<string>(this.defaultCurrency(), { nonNullable: true }),
    smsPaidBy: new FormControl<string>('BUYER', { nonNullable: true }),
    whatsappEnabled: new FormControl<boolean>(true, { nonNullable: true }),
    whatsappAmount: new FormControl<number | null>(5, { validators: [Validators.min(0)] }),
    whatsappCurrency: new FormControl<string>(this.defaultCurrency(), { nonNullable: true }),
    whatsappPaidBy: new FormControl<string>('BUYER', { nonNullable: true }),
    emailEnabled: new FormControl<boolean>(true, { nonNullable: true }),
    emailAmount: new FormControl<number | null>(0, { validators: [Validators.min(0)] }),
    emailCurrency: new FormControl<string>(this.defaultCurrency(), { nonNullable: true }),
    emailPaidBy: new FormControl<string>('TENANT', { nonNullable: true }),
  });

  readonly calendarForm = this.fb.group({
    defaultOpen: new FormControl<boolean>(true, { nonNullable: true }),
    closedWeekdays: new FormControl<string[]>([], { nonNullable: true }),
    holidayTemplateKeys: new FormControl<string[]>([], { nonNullable: true }),
    customHolidayMonthDay: new FormControl<string>('', {
      nonNullable: true,
      validators: [Validators.pattern(/^(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$/)],
    }),
    customHolidayLabel: new FormControl<string>('', { nonNullable: true }),
    customHolidayOpen: new FormControl<boolean>(false, { nonNullable: true }),
  });

  readonly saveLocale = tchMutation<NonNullable<TenantInternalConfig['locale']>, void>({
    run: locale =>
      this.api.updateSettingsSection('locale', locale, { suppressShellFeedback: true }),
    source: 'admin.setup.locale',
    onSuccess: (_result, input) => {
      this.rememberConfig({ ...this.lastConfig, locale: input });
      this.localeForm.markAsPristine();
      this.config.reload();
    },
    onError: err =>
      this.applyFieldErrors(err, this.localeForm, 'admin.setup.locale', LOCALE_FIELD_TARGETS),
  });

  readonly saveReceipt = tchMutation<NonNullable<TenantInternalConfig['document']>, void>({
    run: document =>
      this.api.updateSettingsSection('document', document, { suppressShellFeedback: true }),
    source: 'admin.setup.receipt',
    onSuccess: (_result, input) => {
      this.rememberConfig({ ...this.lastConfig, document: input });
      this.receiptForm.markAsPristine();
      this.config.reload();
    },
    onError: err =>
      this.applyFieldErrors(err, this.receiptForm, 'admin.setup.receipt', RECEIPT_FIELD_TARGETS),
  });

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
    onError: err =>
      this.applyFieldErrors(
        err,
        this.communicationForm,
        'admin.setup.communication',
        COMMUNICATION_FIELD_TARGETS,
      ),
  });

  readonly saveCalendar = tchMutation<NonNullable<TenantInternalConfig['rules']>, void>({
    run: rules => this.api.updateSettingsSection('rules', rules, { suppressShellFeedback: true }),
    source: 'admin.setup.calendar',
    onSuccess: (_result, input) => {
      this.rememberConfig({ ...this.lastConfig, rules: input });
      this.calendarForm.markAsPristine();
      this.config.reload();
    },
    onError: err =>
      this.applyFieldErrors(err, this.calendarForm, 'admin.setup.calendar', CALENDAR_FIELD_TARGETS),
  });

  /** Dernière config connue — base des merges de sauvegarde (value() lance en état error). */
  private lastConfig: TenantInternalConfig = {};

  constructor() {
    // La valeur du resource alimente les formulaires — sans écraser une saisie en cours.
    effect(() => {
      if (!this.config.hasValue()) return;
      const cfg = this.config.value() ?? {};
      const holidayTemplateSnapshot = this.holidayTemplates.hasValue()
        ? this.holidayTemplates.value() ?? []
        : null;
      untracked(() => {
        this.rememberConfig(cfg);
        if (!this.localeForm.dirty) this.patchLocale(cfg);
        if (!this.receiptForm.dirty) this.patchReceipt(cfg);
        if (!this.communicationForm.dirty) this.patchCommunication(cfg);
        if (!this.calendarForm.dirty && holidayTemplateSnapshot) {
          this.patchCalendar(cfg, holidayTemplateSnapshot);
        }
      });
    });

    effect(() => {
      const index = tabIndexFromFragment(this.requestedFragment());
      if (index != null) {
        this.selectedTabIndex.set(index);
      }
    });
  }

  sectionReadiness(section: string): TenantSettingsReadinessSection | null {
    return this.readiness.value()?.sections.find(item => item.section === section) ?? null;
  }

  submitLocale(): void {
    clearServerFieldErrors(this.localeForm);
    this.saveLocale.clearFeedback();
    if (this.localeForm.invalid) {
      this.localeForm.markAllAsTouched();
      return;
    }
    const v = this.localeForm.getRawValue();
    this.saveLocale.execute({
      ...this.lastConfig.locale,
      supportedLanguages: v.supportedLanguages,
      fallbackLanguage: v.fallbackLanguage || null,
    });
  }

  submitReceipt(): void {
    clearServerFieldErrors(this.receiptForm);
    this.saveReceipt.clearFeedback();
    const v = this.receiptForm.getRawValue();
    this.saveReceipt.execute({
      ...this.lastConfig.document,
      receipt: {
        ...this.lastConfig.document?.receipt,
        enabled: v.enabled,
        headerMessage: v.headerMessage || null,
        footerMessage: v.footerMessage || null,
        defaultPaperSize: v.defaultPaperSize || null,
        showQrCode: v.showQrCode,
      },
    });
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
        whatsapp: this.channel(
          v.whatsappEnabled,
          v.whatsappAmount,
          v.whatsappCurrency,
          v.whatsappPaidBy,
        ),
        email: this.channel(v.emailEnabled, v.emailAmount, v.emailCurrency, v.emailPaidBy),
      },
    });
  }

  submitCalendar(): void {
    clearServerFieldErrors(this.calendarForm);
    this.saveCalendar.clearFeedback();
    const v = this.calendarForm.getRawValue();
    this.saveCalendar.execute({
      ...this.lastConfig.rules,
      businessCalendar: {
        ...this.lastConfig.rules?.businessCalendar,
        defaultOpen: v.defaultOpen,
        closedWeekdays: v.closedWeekdays,
        holidays: this.calendarHolidaysFromForm(),
      },
    });
  }

  addCustomHoliday(): void {
    const v = this.calendarForm.getRawValue();
    const monthDay = v.customHolidayMonthDay.trim();
    const label = v.customHolidayLabel.trim();
    this.calendarForm.controls.customHolidayMonthDay.markAsTouched();
    this.calendarForm.controls.customHolidayLabel.markAsTouched();
    this.calendarForm.controls.customHolidayLabel.setErrors(null);
    if (this.calendarForm.controls.customHolidayMonthDay.invalid || !label) {
      if (!label) {
        this.calendarForm.controls.customHolidayLabel.setErrors({ required: true });
      }
      return;
    }

    const key = `custom_${monthDay.replace('-', '_')}_${slug(label)}`;
    this.customHolidays.update(items => [
      ...items.filter(item => item.key !== key),
      { key, monthDay, label, open: v.customHolidayOpen },
    ]);
    this.calendarForm.patchValue({
      customHolidayMonthDay: '',
      customHolidayLabel: '',
      customHolidayOpen: false,
    });
    this.calendarForm.markAsDirty();
  }

  removeCustomHoliday(key: string | null | undefined): void {
    if (!key) return;
    this.customHolidays.update(items => items.filter(item => item.key !== key));
    this.calendarForm.markAsDirty();
  }

  private patchLocale(cfg: TenantInternalConfig): void {
    const loc = cfg.locale;
    if (!loc) return;
    this.localeForm.patchValue({
      supportedLanguages: loc.supportedLanguages ?? [
        ...this.runtimeSettings.tenantSupportedLanguages(),
      ],
      fallbackLanguage:
        loc.fallbackLanguage ?? this.runtimeSettings.tenantSupportedLanguages()[0] ?? 'ht',
    });
  }

  private patchReceipt(cfg: TenantInternalConfig): void {
    const r = cfg.document?.receipt;
    if (!r) return;
    this.receiptForm.patchValue({
      enabled: r.enabled ?? true,
      headerMessage: r.headerMessage ?? '',
      footerMessage: r.footerMessage ?? '',
      defaultPaperSize: r.defaultPaperSize ?? 'RECEIPT_80MM',
      showQrCode: r.showQrCode ?? true,
    });
  }

  private patchCommunication(cfg: TenantInternalConfig): void {
    const delivery = cfg.communication?.buyerTicketDelivery;
    if (!delivery) return;
    this.communicationForm.patchValue({
      smsEnabled: delivery.sms?.enabled ?? true,
      smsAmount: delivery.sms?.amount ?? null,
      smsCurrency: delivery.sms?.currency ?? this.defaultCurrency(),
      smsPaidBy: delivery.sms?.paidBy ?? 'BUYER',
      whatsappEnabled: delivery.whatsapp?.enabled ?? true,
      whatsappAmount: delivery.whatsapp?.amount ?? null,
      whatsappCurrency: delivery.whatsapp?.currency ?? this.defaultCurrency(),
      whatsappPaidBy: delivery.whatsapp?.paidBy ?? 'BUYER',
      emailEnabled: delivery.email?.enabled ?? true,
      emailAmount: delivery.email?.amount ?? null,
      emailCurrency: delivery.email?.currency ?? this.defaultCurrency(),
      emailPaidBy: delivery.email?.paidBy ?? 'TENANT',
    });
  }

  private patchCalendar(cfg: TenantInternalConfig, templates: readonly { key: string }[]): void {
    const c = cfg.rules?.businessCalendar;
    if (!c) return;
    this.calendarForm.patchValue({
      defaultOpen: c.defaultOpen ?? true,
      closedWeekdays: c.closedWeekdays ?? [],
      holidayTemplateKeys: (c.holidays ?? [])
        .filter(holiday => this.templateKeySet(templates).has(holiday.key ?? ''))
        .map(holiday => holiday.key)
        .filter((key): key is string => !!key),
    });
    this.customHolidays.set(
      (c.holidays ?? []).filter(holiday => !this.templateKeySet(templates).has(holiday.key ?? '')),
    );
  }

  private channel(enabled: boolean, amount: number | null, currency: string, paidBy: string) {
    return enabled
      ? {
          enabled,
          amount: amount ?? 0,
          currency: currency || this.defaultCurrency(),
          paidBy: paidBy || 'BUYER',
        }
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

  private calendarHolidaysFromForm(): TenantRecurringHoliday[] {
    const templateByKey = new Map(
      (this.holidayTemplates.value() ?? []).map(template => [template.key, template]),
    );
    const selectedTemplates = this.calendarForm
      .getRawValue()
      .holidayTemplateKeys.map(key => templateByKey.get(key))
      .filter((template): template is NonNullable<typeof template> => !!template)
      .map(template => ({
        key: template.key,
        monthDay: template.monthDay,
        open: template.defaultOpen,
        label: template.label,
      }));

    return [...selectedTemplates, ...this.customHolidays()];
  }

  private templateKeySet(templates: readonly { key: string }[]): ReadonlySet<string> {
    return new Set(templates.map(template => template.key));
  }

  /** Applique les erreurs serveur par champ ; `true` = tout est géré, pas de feedback générique. */
  private applyFieldErrors(
    err: unknown,
    form:
      | typeof this.localeForm
      | typeof this.receiptForm
      | typeof this.communicationForm
      | typeof this.calendarForm,
    source: string,
    targets: Record<string, string>,
  ): boolean {
    const problem = mapHttpErrorToProblemDetail(err);

    const fieldErrors = withResolvedErrorCopies(
      webAppErrorsFromProblemDetailFields(problem, source),
      key => this.translate.instant(key),
    );
    const remaining = applyServerFieldErrors(form, fieldErrors, targets);
    return fieldErrors.length > 0 && remaining.length === 0;
  }
}

function slug(value: string): string {
  return (
    value
      .trim()
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '') || 'holiday'
  );
}

function tabIndexFromFragment(fragment: string | null): number | null {
  switch ((fragment ?? '').trim().toLowerCase()) {
    case 'print':
    case 'receipt':
      return 0;
    case 'send':
    case 'communication':
      return 1;
    case 'calendar':
    case 'business-calendar':
      return 2;
    case 'languages':
    case 'locale':
      return 3;
    default:
      return null;
  }
}
