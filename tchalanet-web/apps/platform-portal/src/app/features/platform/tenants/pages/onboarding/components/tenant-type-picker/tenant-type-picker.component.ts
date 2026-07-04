import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { TenantType } from '../../../../data-access/platform-provisioning-api.service';

export interface TenantTypeOption {
  readonly value: TenantType;
  readonly label: string;
  readonly icon: string;
  readonly descriptionKey: string;
}

@Component({
  selector: 'tch-tenant-type-picker',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe],
  templateUrl: './tenant-type-picker.component.html',
  styleUrls: ['./tenant-type-picker.component.scss'],
})
export class TenantTypePickerComponent {
  readonly options = input.required<readonly TenantTypeOption[]>();
  readonly selected = input<TenantType | '' | null>('');
  readonly invalid = input(false);
  readonly selectedChange = output<TenantType>();

  select(value: TenantType): void {
    this.selectedChange.emit(value);
  }
}
