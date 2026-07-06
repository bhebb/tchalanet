import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

export interface ConsoleAddressSummary {
  readonly line1?: string | null;
  readonly line2?: string | null;
  readonly city?: string | null;
  readonly region?: string | null;
  readonly country?: string | null;
  readonly postalCode?: string | null;
}

interface AddressFact {
  readonly labelKey: string;
  readonly value: string;
}

@Component({
  selector: 'tch-console-address-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe],
  templateUrl: './console-address-summary.component.html',
  styleUrls: ['./console-address-summary.component.scss'],
})
export class ConsoleAddressSummaryComponent {
  readonly address = input<ConsoleAddressSummary | null | undefined>(null);
  readonly emptyKey = input('console.addressSummary.empty');

  readonly facts = computed<readonly AddressFact[]>(() => {
    const address = this.address();
    if (!address) return [];

    return [
      { labelKey: 'console.address.field.line1', value: address.line1 },
      { labelKey: 'console.address.field.line2', value: address.line2 },
      { labelKey: 'console.address.field.city', value: address.city },
      { labelKey: 'console.address.field.region', value: address.region },
      { labelKey: 'console.address.field.country', value: address.country },
      { labelKey: 'console.address.field.postalCode', value: address.postalCode },
    ].flatMap(fact => {
      const value = fact.value?.trim();
      return value ? [{ labelKey: fact.labelKey, value }] : [];
    });
  });
}
