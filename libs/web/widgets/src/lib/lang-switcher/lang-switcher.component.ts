import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatMenu, MatMenuItem, MatMenuTrigger } from '@angular/material/menu';
import { MatIconButton } from '@angular/material/button';

@Component({
  selector: 'tchl-lang-switcher',
  standalone: true,
  imports: [MatCardModule, MatIconModule, MatMenu, MatMenuItem, MatMenuTrigger, MatIconButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './lang-switcher.component.html',
  styleUrls: ['./lang-switcher.component.scss'],
})
export class LangSwitcherComponent {
  availableLangs = input.required<string[]>();
  currentLang = input.required<string>();
  labelFor = input<(l: string) => string>(l => l);
  change = output<string>();

  changeLanguage(l: string) {
    if (l !== this.currentLang()) {
      this.change.emit(l);
    }
  }
  flagIcon = (l: string) => `/assets/svg/i18n/${l}.svg`;
}
