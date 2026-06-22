import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

export interface AdminNextStep {
  icon: string;
  label: string;
  routerLink?: string[];
  href?: string;
}

@Component({
  selector: 'tch-admin-next-steps-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  templateUrl: './admin-next-steps-card.component.html',
  styleUrls: ['./admin-next-steps-card.component.scss'],
})
export class AdminNextStepsCardComponent {
  readonly title = input('Prochaines étapes');
  readonly steps = input.required<AdminNextStep[]>();
}
