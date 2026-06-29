import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';

export interface TenantLocaleConfig {
  defaultLanguage?: string | null;
  defaultLocale?: string | null;
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
  displayName?: string | null;
  headerMessage?: string | null;
  footerMessage?: string | null;
  defaultPaperSize?: string | null;
  showQrCode?: boolean | null;
  showSellerName?: boolean | null;
  showOutletName?: boolean | null;
  showPotentialPayout?: boolean | null;
  defaultTemplateKey?: string | null;
}

export interface TenantDocumentConfig {
  receipt?: TenantReceiptConfig | null;
}

export interface TenantBusinessCalendar {
  defaultOpen?: boolean | null;
  closedWeekdays?: string[] | null;
  holidaySalesAllowed?: boolean | null;
}

export interface TenantRulesConfig {
  businessCalendar?: TenantBusinessCalendar | null;
}

export interface TenantInternalConfig {
  locale?: TenantLocaleConfig | null;
  communication?: TenantCommunicationConfig | null;
  document?: TenantDocumentConfig | null;
  rules?: TenantRulesConfig | null;
}

@Injectable({ providedIn: 'root' })
export class TenantConfigApiService {
  private readonly backend = inject(TchBackendClient);

  getTenantConfig(options?: TchRequestOptions): Observable<TenantInternalConfig> {
    return this.backend.get<TenantInternalConfig>('/admin/tenant-config', options);
  }

  updateInternalSettings(req: TenantInternalConfig, options?: TchRequestOptions): Observable<void> {
    return this.backend.put<void>('/admin/tenant-config/internal-settings', req, options);
  }

  getCommunicationConfig(options?: TchRequestOptions): Observable<TenantCommunicationConfig> {
    return this.backend.get<TenantCommunicationConfig>('/admin/tenant-config/communication', options);
  }

  getDocumentConfig(options?: TchRequestOptions): Observable<TenantDocumentConfig> {
    return this.backend.get<TenantDocumentConfig>('/admin/tenant-config/document', options);
  }
}
