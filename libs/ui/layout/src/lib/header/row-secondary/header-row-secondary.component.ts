import { TranslatePipe } from '@ngx-translate/core';

import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { ThemeMode } from '@tchl/ui/theme';

import { LangThemeGroupComponent } from '../../lang-theme-group/lang-theme-group.component';
import { NavComponent } from '../../nav/nav.component';
import { HeaderPublicVm } from '../header-public.viewmodel';


@Component({
  selector: 'tchl-header-row-secondary',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    TranslatePipe,
    LangThemeGroupComponent,
    NavComponent,
  ],
  templateUrl: './header-row-secondary.component.html',
  styles: [''],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HeaderRowSecondaryComponent {
  vm = input.required<HeaderPublicVm>();

  navSelect = output<string>();
  mobileCTAClick = output<void>();
  searchClick = output<void>();
  changeLang = output<string>();
  toggleTheme = output<ThemeMode>();
}
