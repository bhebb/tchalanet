import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { FormField } from '@angular/forms/signals';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminSectionCardComponent } from '@tch/ui/console';

/**
 * Section de formulaire adresse partagée (signal forms).
 *
 * `form` reçoit le FieldTree adresse NON invoqué (ex. `[form]="form().address"` ou
 * `[form]="addressForm"`) — jamais `addressForm()` : le composant l'invoque lui-même
 * pour lire l'état des champs.
 */
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
  readonly titleKey = input('console.address.title');
  readonly descriptionKey = input<string | null>('console.address.description');
  readonly icon = input('location_on');
  readonly required = input(false);
  readonly framed = input(true);
}
