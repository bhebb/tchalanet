import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'tch-seller-terminal-preview-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe],
  templateUrl: './seller-terminal-preview-card.component.html',
  styleUrls: ['./seller-terminal-preview-card.component.scss'],
})
export class SellerTerminalPreviewCardComponent {
  readonly terminalCode = input('');
  readonly displayName = input('');
  readonly firstName = input('');
  readonly lastName = input('');
  readonly phoneNumber = input('');
  readonly active = input(false);
  readonly commissionRate = input<number | null>(null);

  fullName(): string {
    return `${this.firstName()} ${this.lastName()}`.trim();
  }
}
