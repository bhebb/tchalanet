import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

export type ConsolePersonIdentitySummaryLayout = 'block' | 'inline';

export interface ConsolePersonIdentitySummaryField {
  readonly labelKey: string;
  readonly value?: string | null;
  readonly required?: boolean;
  readonly readonly?: boolean;
  readonly hidden?: boolean;
  readonly emptyKey?: string;
}

export interface ConsolePersonIdentitySummary {
  readonly displayName?: string | null;
  readonly firstName?: string | null;
  readonly lastName?: string | null;
  readonly email?: string | null;
  readonly phoneNumber?: string | null;
}

interface ResolvedIdentityField {
  readonly labelKey: string;
  readonly value?: string;
  readonly valueKey?: string;
  readonly required: boolean;
  readonly readonly: boolean;
}

@Component({
  selector: 'tch-console-person-identity-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe],
  templateUrl: './console-person-identity-summary.component.html',
  styleUrls: ['./console-person-identity-summary.component.scss'],
})
export class ConsolePersonIdentitySummaryComponent {
  readonly identity = input<ConsolePersonIdentitySummary | null | undefined>(null);
  readonly fields = input<readonly ConsolePersonIdentitySummaryField[] | null>(null);
  readonly layout = input<ConsolePersonIdentitySummaryLayout>('block');
  readonly emptyKey = input('component.console.personIdentitySummary.empty');

  readonly resolvedFields = computed<readonly ResolvedIdentityField[]>(() => {
    const fields = this.fields() ?? this.defaultFields();

    return fields.flatMap((field): readonly ResolvedIdentityField[] => {
      if (field.hidden) return [];
      const value = field.value?.trim();
      if (value) {
        return [{
          labelKey: field.labelKey,
          value,
          required: !!field.required,
          readonly: !!field.readonly,
        }];
      }
      if (field.required) {
        return [{
          labelKey: field.labelKey,
          valueKey: field.emptyKey ?? 'common.not_set',
          required: true,
          readonly: !!field.readonly,
        }];
      }
      return [];
    });
  });

  private defaultFields(): readonly ConsolePersonIdentitySummaryField[] {
    const identity = this.identity();
    if (!identity) return [];
    const displayName = identity.displayName?.trim() ||
      [identity.firstName, identity.lastName].filter(Boolean).join(' ').trim();

    return [
      {
        labelKey: 'component.console.personIdentity.field.displayName',
        value: displayName,
        required: true,
        readonly: true,
      },
      {
        labelKey: 'component.console.personIdentity.field.email',
        value: identity.email,
        required: true,
        readonly: true,
      },
      {
        labelKey: 'component.console.personIdentity.field.phoneNumber',
        value: identity.phoneNumber,
        readonly: true,
      },
    ];
  }
}
