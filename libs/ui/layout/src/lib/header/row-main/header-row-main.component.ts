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
import { MatMenuModule } from '@angular/material/menu';

import { ThemeMode } from '@tchl/ui/theme';

import { BrandComponent } from '../../brand/brand.component';
import { LangThemeGroupComponent } from '../../lang-theme-group/lang-theme-group.component';
import { NavComponent } from '../../nav/nav.component';
import { HeaderPublicVm } from '../header-public.viewmodel';


@Component({
  selector: 'tchl-header-row-main',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatMenuModule,
    MatButtonModule,
    TranslatePipe,
    BrandComponent,
    LangThemeGroupComponent,
    NavComponent,
    LangThemeGroupComponent,
  ],
  templateUrl: './header-row-main.component.html',
  styles: [''],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HeaderRowMainComponent {
  vm = input.required<HeaderPublicVm>();

  // Events (intentions)
  burgerClick = output<void>();
  brandClick = output<void>();
  ctaClick = output<void>();
  navSelect = output<string>();
  searchClick = output<void>();
  changeLang = output<string>();
  toggleTheme = output<ThemeMode>();
  accountClick = output<void>();
  logoutClick = output<void>();
}
