import { Injectable, ResourceRef, inject } from '@angular/core';
import { TchBackendClient, TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';

export interface TenantLocaleConfig {
  supportedLanguages?: string[] | null;
  fallbackLanguage?: string | null;
}

export interface TenantDeliveryChannel {
  enabled?: boolean | null;
  amount?: number | null;
  currency?: string | null;
  paidBy?: string | null;
}

export interface TenantCommunicationConfig {
  buyerTicketDelivery?: {
    sms?: TenantDeliveryChannel | null;
    whatsapp?: TenantDeliveryChannel | null;
    email?: TenantDeliveryChannel | null;
  } | null;
}

export interface TenantReceiptConfig {
  enabled?: boolean | null;
  headerMessage?: string | null;
  footerMessage?: string | null;
  defaultPaperSize?: string | null;
  showQrCode?: boolean | null;
  defaultTemplateKey?: string | null;
}

export interface TenantDocumentConfig {
  receipt?: TenantReceiptConfig | null;
}

export interface TenantBusinessCalendar {
  defaultOpen?: boolean | null;
  closedWeekdays?: string[] | null;
  holidays?: TenantRecurringHoliday[] | null;
}

export interface TenantRulesConfig {
  promotions?: TenantPromotionRules | null;
  businessCalendar?: TenantBusinessCalendar | null;
}

export interface TenantPromotionRules {
  maryajGratisEnabled?: boolean | null;
}

export interface TenantRecurringHoliday {
  key?: string | null;
  monthDay?: string | null;
  open?: boolean | null;
  label?: string | null;
}

export interface TenantHolidayTemplate {
  key: string;
  countryCode: string;
  monthDay: string;
  defaultOpen: boolean;
  label: string;
}

export interface TenantInternalConfig {
  locale?: TenantLocaleConfig | null;
  communication?: TenantCommunicationConfig | null;
  document?: TenantDocumentConfig | null;
  rules?: TenantRulesConfig | null;
}

export type TenantConfigSection = 'communication' | 'document' | 'rules' | 'locale';

export interface TenantSettingsReadinessSection {
  section: string;
  status: 'READY' | 'ACTION_REQUIRED';
  blockingReasons: string[];
  warnings: string[];
}

export interface TenantSettingsReadiness {
  ready: boolean;
  sections: TenantSettingsReadinessSection[];
}

export function tenantMaryajGratisEnabled(
  config: TenantInternalConfig | null | undefined,
): boolean {
  return config?.rules?.promotions?.maryajGratisEnabled ?? true;
}

@Injectable({ providedIn: 'root' })
export class TenantParametersApiService {
  private readonly backend = inject(TchBackendClient);

  getTenantConfig(options?: TchRequestOptions): Observable<TenantInternalConfig | null> {
    return this.backend.get<TenantInternalConfig | null>('/admin/tenant-config', options);
  }

  /** Resource de lecture de la config tenant (chargement + reload déclaratifs). */
  tenantConfigResource(): ResourceRef<TenantInternalConfig | null | undefined> {
    return this.backend.getResource<TenantInternalConfig | null>(() => ({
      path: '/admin/tenant-config',
      options: { suppressShellFeedback: true },
    }));
  }

  updateInternalSettings(req: TenantInternalConfig, options?: TchRequestOptions): Observable<void> {
    return this.backend.put<void>('/admin/tenant-config/internal-settings', req, options);
  }

  updateSettingsSection(
    section: TenantConfigSection,
    value: TenantInternalConfig[TenantConfigSection],
    options?: TchRequestOptions,
  ): Observable<void> {
    return this.backend.put<void>(`/admin/tenant-config/sections/${section}`, value ?? {}, options);
  }

  getSettingsSection<T extends TenantConfigSection>(
    section: T,
    options?: TchRequestOptions,
  ): Observable<NonNullable<TenantInternalConfig[T]>> {
    return this.backend.get<NonNullable<TenantInternalConfig[T]>>(
      `/admin/tenant-config/sections/${section}`,
      options,
    );
  }

  getReadiness(options?: TchRequestOptions): Observable<TenantSettingsReadiness> {
    return this.backend.get<TenantSettingsReadiness>('/admin/tenant-config/readiness', options);
  }

  holidayTemplatesResource(): ResourceRef<TenantHolidayTemplate[] | undefined> {
    return this.backend.getResource<TenantHolidayTemplate[]>(() => ({
      path: '/admin/tenant-config/holiday-templates',
      options: { suppressShellFeedback: true },
    }));
  }

  getCommunicationConfig(options?: TchRequestOptions): Observable<TenantCommunicationConfig> {
    return this.backend.get<TenantCommunicationConfig>(
      '/admin/tenant-config/communication',
      options,
    );
  }

  getDocumentConfig(options?: TchRequestOptions): Observable<TenantDocumentConfig> {
    return this.backend.get<TenantDocumentConfig>('/admin/tenant-config/document', options);
  }
}
