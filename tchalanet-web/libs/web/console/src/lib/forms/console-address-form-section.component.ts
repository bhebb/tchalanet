import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { FormField } from '@angular/forms/signals';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminSectionCardComponent } from '@tch/ui/console';

@Component({
  selector: 'tch-console-address-form-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    NgTemplateOutlet,
    FormField,
    AdminSectionCardComponent,
    TranslatePipe,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './console-address-form-section.component.html',
  styleUrls: ['./console-form-sections.scss'],
})
export class ConsoleAddressFormSectionComponent {
  readonly form = input.required<any>();
  readonly titleKey = input('component.console.address.title');
  readonly descriptionKey = input<string | null>('component.console.address.description');
  readonly icon = input('location_on');
  readonly required = input(false);
  readonly framed = input(true);
}
