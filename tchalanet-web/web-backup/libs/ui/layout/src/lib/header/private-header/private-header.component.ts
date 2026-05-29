import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';

import { I18nFacade } from '@tchl/facades';
import { HeaderProperties } from '@tchl/types';
import { ThemeMode, ThemeService } from '@tchl/ui/theme';

import { BrandComponent } from '../../brand/brand.component';
import { TchBreakpointService } from '../../breakpoints/breakpoint.service';
import { LangThemeGroupComponent } from '../../lang-theme-group/lang-theme-group.component';

@Component({
  selector: 'tchl-private-header',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule, MatMenuModule, LangThemeGroupComponent, BrandComponent],
  templateUrl: './private-header.component.html',
  styleUrl: './private-header.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PrivateHeaderComponent {
  properties = input.required<HeaderProperties>();
  user = input.required<{
    displayName?: string;
    email?: string;
    avatarUrl?: string;
    roles?: string[];
    tenantId?: string;
    plan?: string;
  }>();
  flags = input<string[]>([]);
  toggleSidebar = output<void>();
  changeLang = output<string>();
  toggleTheme = output<ThemeMode>();
  searchClick = output<void>();
  signOut = output<void>();
  // avatar initials fallback
  initials = computed(() => {
    const u = this.user();
    const src = u.displayName || u.email || '';
    const parts = src
      .split(/[^\p{L}\p{N}]+/u)
      .filter(Boolean)
      .slice(0, 2);
    return parts.map(w => w[0].toUpperCase()).join('');
  });
  vm = computed(() => {
    const handset = this.handset();
    const tablet = this.tablet();
    const desktop = this.desktop();

    const props = this.properties();

    const showSearch = !(
      !this.flags().includes(props.actions?.search?.flag ?? '') && props.actions?.search?.flag
    );
    const showLang = !(
      !this.flags().includes(props.actions?.lang?.flag ?? '') && props.actions?.lang?.flag
    );
    const showTheme = !(
      !this.flags().includes(props.actions?.theme?.flag ?? '') && props.actions?.theme?.flag
    );
    const showLangTheme = showLang || showTheme;

    return {
      handset,
      tablet,
      desktop,

      brand: props.brand,

      showSearch,
      showLangTheme,

      // mobile:
      // L1: burger | brand | avatar
      // L2: search + lang/theme
      showRow2MobileTools: handset,

      // tablet/desktop:
      // Single row: burger | brand | search | lang/theme | avatar
    };
  });
  // feature flags globals
  private readonly breakpoint = inject(TchBreakpointService);
  handset = this.breakpoint.handset;
  tablet = this.breakpoint.tablet;
  desktop = this.breakpoint.desktop;
  private readonly router = inject(Router);
  private readonly i18n = inject(I18nFacade);
  currentLang = this.i18n.current;
  availableLangs = this.i18n.available;
  private readonly theme = inject(ThemeService);
  themeMode = this.theme.mode;
  // private readonly analytics = inject(AnalyticsService);

  onToggleSidebar() {
    this.toggleSidebar.emit();
  }

  onBrandClick() {
    this.router.navigateByUrl('/app');
  }

  /** Navigation vers la page profil / compte privé */
  onAccountClick() {
    const props = this.properties();
    const target = props?.account?.private?.path;
    if (target) {
      this.router.navigateByUrl(target);
    }
  }

  /** Déclenchement du logout vers le shell */
  onSignOutClick() {
    this.signOut.emit();
  }

  /* ------------------------------------------------------------------
   * Derived UI bits
   * ------------------------------------------------------------------ */

  onSearchClick() {
    this.searchClick.emit();
  }

  onChangeLang(lang: string) {
    this.i18n.setCurrent(lang);
    this.changeLang.emit(lang);
  }

  onToggleTheme(mode: ThemeMode) {
    this.theme.setMode(mode);
    this.toggleTheme.emit(mode);
  }
}
