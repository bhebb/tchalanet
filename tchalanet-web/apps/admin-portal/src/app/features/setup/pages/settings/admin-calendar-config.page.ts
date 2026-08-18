import {
  ChangeDetectionStrategy,
  Component,
  effect,
  inject,
  signal,
  untracked,
} from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
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
  TenantRecurringHoliday,
} from '../../data-access/tenant-parameters-api.service';

const WEEKDAYS = [
  { code: 'MONDAY', labelKey: 'admin.settings.config.calendar.days.monday' },
  { code: 'TUESDAY', labelKey: 'admin.settings.config.calendar.days.tuesday' },
  { code: 'WEDNESDAY', labelKey: 'admin.settings.config.calendar.days.wednesday' },
  { code: 'THURSDAY', labelKey: 'admin.settings.config.calendar.days.thursday' },
  { code: 'FRIDAY', labelKey: 'admin.settings.config.calendar.days.friday' },
  { code: 'SATURDAY', labelKey: 'admin.settings.config.calendar.days.saturday' },
  { code: 'SUNDAY', labelKey: 'admin.settings.config.calendar.days.sunday' },
] as const;

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
  selector: 'tch-admin-calendar-config-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    TranslatePipe,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchAsyncViewComponent,
    TchAsyncReadyDirective,
    TchFieldError,
    TchNotice,
  ],
  templateUrl: './admin-calendar-config.page.html',
  styleUrls: ['./admin-calendar-config.page.scss'],
})
export class AdminCalendarConfigPage {
  private readonly api = inject(TenantParametersApiService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly runtimeSettings = inject(RuntimeSettingsStore);
  private readonly translate = inject(TranslateService);

  readonly fromSetup = this.route.snapshot.queryParamMap.get('from') === 'setup';
  readonly backRoute = this.fromSetup ? '/app/admin/setup' : '/app/admin/company/settings';
  readonly backLabel = this.fromSetup ? 'admin.setup.backToSetup' : 'admin.settings.title';

  readonly weekdays = WEEKDAYS;
  readonly config = this.api.tenantConfigResource();
  readonly holidayTemplates = this.api.holidayTemplatesResource();
  readonly configError = resourceErrorVm(this.config, 'admin.setup.config');
  readonly configIsEmpty = () => false;
  readonly readiness = this.api.readinessResource();
  readonly customHolidays = signal<TenantRecurringHoliday[]>([]);

  private lastConfig: TenantInternalConfig = {};

  readonly calendarForm = this.fb.group({
    defaultOpen: new FormControl<boolean>(true, { nonNullable: true }),
    closedWeekdays: new FormControl<string[]>([], { nonNullable: true }),
    holidayTemplateKeys: new FormControl<string[]>([], { nonNullable: true }),
    customHolidayMonthDay: new FormControl<string>('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/^(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$/)],
    }),
    customHolidayLabel: new FormControl<string>('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    customHolidayOpen: new FormControl<boolean>(false, { nonNullable: true }),
  });

  readonly saveCalendar = tchMutation<NonNullable<TenantInternalConfig['rules']>, void>({
    run: rules => this.api.updateSettingsSection('rules', rules, { suppressShellFeedback: true }),
    source: 'admin.setup.calendar',
    onSuccess: (_result, input) => {
      this.rememberConfig({ ...this.lastConfig, rules: input });
      this.calendarForm.markAsPristine();
      this.config.reload();
    },
    onError: err => this.applyFieldErrors(err),
  });

  constructor() {
    effect(() => {
      if (!this.config.hasValue()) return;
      const cfg = this.config.value() ?? {};
      const holidayTemplateSnapshot = this.holidayTemplates.hasValue()
        ? this.holidayTemplates.value() ?? []
        : null;
      untracked(() => {
        this.rememberConfig(cfg);
        if (!this.calendarForm.dirty && holidayTemplateSnapshot) {
          this.patchForm(cfg, holidayTemplateSnapshot);
        }
      });
    });
  }

  sectionReadiness(section: string) {
    return this.readiness.value()?.sections.find(item => item.section === section) ?? null;
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
    if (this.calendarForm.controls.customHolidayMonthDay.invalid ||
        this.calendarForm.controls.customHolidayLabel.invalid) {
      return;
    }
    const key = `custom_${monthDay.replace('-', '_')}_${slug(label)}`;
    this.customHolidays.update(items => [
      ...items.filter(item => item.key !== key),
      { key, monthDay, label, open: v.customHolidayOpen },
    ]);
    this.calendarForm.patchValue({ customHolidayMonthDay: '', customHolidayLabel: '', customHolidayOpen: false });
    this.calendarForm.markAsDirty();
  }

  removeCustomHoliday(key: string | null | undefined): void {
    if (!key) return;
    this.customHolidays.update(items => items.filter(item => item.key !== key));
    this.calendarForm.markAsDirty();
  }

  private patchForm(cfg: TenantInternalConfig, templates: readonly { key: string }[]): void {
    const c = cfg.rules?.businessCalendar;
    if (!c) return;
    this.calendarForm.patchValue({
      defaultOpen: c.defaultOpen ?? true,
      closedWeekdays: c.closedWeekdays ?? [],
      holidayTemplateKeys: (c.holidays ?? [])
        .filter(h => this.templateKeySet(templates).has(h.key ?? ''))
        .map(h => h.key)
        .filter((k): k is string => !!k),
    });
    this.customHolidays.set(
      (c.holidays ?? []).filter(h => !this.templateKeySet(templates).has(h.key ?? '')),
    );
  }

  private calendarHolidaysFromForm(): TenantRecurringHoliday[] {
    const templateByKey = new Map(
      (this.holidayTemplates.value() ?? []).map(t => [t.key, t]),
    );
    const selected = this.calendarForm.getRawValue().holidayTemplateKeys
      .map(key => templateByKey.get(key))
      .filter((t): t is NonNullable<typeof t> => !!t)
      .map(t => ({ key: t.key, monthDay: t.monthDay, open: t.defaultOpen, label: t.label }));
    return [...selected, ...this.customHolidays()];
  }

  private templateKeySet(templates: readonly { key: string }[]): ReadonlySet<string> {
    return new Set(templates.map(t => t.key));
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
      webAppErrorsFromProblemDetailFields(problem, 'admin.setup.calendar'),
      key => this.translate.instant(key),
    );
    const remaining = applyServerFieldErrors(this.calendarForm, fieldErrors, CALENDAR_FIELD_TARGETS);
    return fieldErrors.length > 0 && remaining.length === 0;
  }
}

function slug(value: string): string {
  return (
    value.trim().toLowerCase()
      .normalize('NFD').replace(/[̀-ͯ]/g, '')
      .replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '') || 'holiday'
  );
}
