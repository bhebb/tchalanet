import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import {
  AdminNextStep,
  AdminNextStepsCardComponent,
  AdminProvisioningHealthCardComponent,
  AdminProvisioningHealthError,
  AdminSectionCardComponent,
  AdminStatusTone,
  TchIdentityCardComponent,
} from '@tch/ui/console';

import {
  TenantProvisioningPreviewView,
  TenantProvisioningResultView,
} from '../../../../data-access/platform-provisioning-api.service';
import {
  DOMAIN_LABEL_KEYS,
  STATUS_LABEL_KEYS,
  STEP_ICONS,
  STEP_LABEL_KEYS,
  domainTone,
  translatedLabel,
  warningLabel,
} from '../tenant-provisioning-display';

export interface TenantProvisioningIdentityMeta {
  readonly label: string;
  readonly value: string;
}

@Component({
  selector: 'tch-tenant-provisioning-aside',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    TchIdentityCardComponent,
    AdminProvisioningHealthCardComponent,
    AdminNextStepsCardComponent,
    AdminSectionCardComponent,
  ],
  templateUrl: './tenant-provisioning-aside.component.html',
  styleUrls: ['./tenant-provisioning-aside.component.scss'],
})
export class TenantProvisioningAsideComponent {
  private readonly translate = inject(TranslateService);

  readonly submitted = input(false);
  readonly tenantName = input<string | null | undefined>(null);
  readonly tenantCode = input<string | null | undefined>(null);
  readonly identityMeta = input<readonly TenantProvisioningIdentityMeta[]>([]);
  readonly preview = input<TenantProvisioningPreviewView | null>(null);
  readonly result = input<TenantProvisioningResultView | null>(null);
  readonly previewLoading = input(false);
  readonly previewError = input<AdminProvisioningHealthError | null>(null);

  readonly status = computed(() => this.result()?.readiness?.status || 'DRAFT');
  readonly statusTone = computed((): AdminStatusTone => this.submitted() ? 'success' : 'neutral');

  readonly readinessLabel = computed(() => {
    const result = this.result();
    if (result) return result.readiness.status;
    if (this.preview()) return this.translate.instant('platform.tenantProvisioning.status.previewed');
    return this.translate.instant('platform.tenantProvisioning.status.toValidate');
  });

  readonly readinessTone = computed((): AdminStatusTone => {
    const resultStatus = this.result()?.readiness.status;
    if (resultStatus === 'READY') return 'success';
    if (resultStatus === 'INCOMPLETE') return 'warning';
    if (resultStatus === 'MISSING') return 'danger';
    if (this.previewError()) return 'danger';
    if (this.preview()) return 'info';
    return 'neutral';
  });

  readonly healthProgress = computed(() => {
    const sections = this.result()?.readiness.sections ?? [];
    if (!sections.length) return null;
    const ready = sections.filter(section => domainTone(section.status) === 'success').length;
    return Math.round((ready / sections.length) * 100);
  });

  readonly healthItems = computed(() => {
    const domainStatuses = this.result()?.domainStatuses;
    if (domainStatuses && Object.keys(domainStatuses).length) {
      return Object.entries(domainStatuses).map(([key, value]) => ({
        label: translatedLabel(this.translate, DOMAIN_LABEL_KEYS[key] ?? key),
        status: translatedLabel(this.translate, STATUS_LABEL_KEYS[String(value)] ?? String(value)),
        tone: domainTone(String(value)),
      }));
    }

    const preview = this.preview();
    if (!preview) return [];

    const domains = preview.includedDomains.map(domain => ({
      label: translatedLabel(this.translate, DOMAIN_LABEL_KEYS[domain] ?? domain),
      status: this.translate.instant('platform.tenantProvisioning.status.planned'),
      tone: 'info' as AdminStatusTone,
    }));
    const sections = preview.expectedReadinessSections.map(section => ({
      label: translatedLabel(this.translate, DOMAIN_LABEL_KEYS[section] ?? section),
      status: this.translate.instant('platform.tenantProvisioning.status.expected'),
      tone: 'neutral' as AdminStatusTone,
    }));

    return [...domains, ...sections];
  });

  readonly nextStepsItems = computed((): AdminNextStep[] => {
    const result = this.result();
    if (!result?.nextSteps?.length) return [];
    return result.nextSteps.map(step => ({
      icon: STEP_ICONS[step] ?? 'arrow_forward',
      label: this.translate.instant(STEP_LABEL_KEYS[step] ?? step),
      routerLink: step === 'CREATE_INITIAL_ADMIN' && result.tenantId
        ? ['/app/platform/tenants', result.tenantId, 'admins', 'new']
        : undefined,
    }));
  });

  readonly previewWarningLabels = computed(() =>
    (this.preview()?.warnings ?? []).map(warning => warningLabel(this.translate, warning)),
  );

  readonly resultWarningLabels = computed(() =>
    (this.result()?.warnings ?? []).map(warning => warningLabel(this.translate, warning)),
  );
}
