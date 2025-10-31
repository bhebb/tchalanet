import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

import { AnalyticsService } from '@tchl/analytics';
import { environment } from '@tchl/config';
import { I18nFacade } from '@tchl/facades';
import { FeatureService } from '@tchl/feature';
import { OverlayService } from '@tchl/search';
import { HeaderProperties } from '@tchl/types';
import { ThemeMode, ThemeService } from '@tchl/ui/theme';

import { TchBreakpointService } from '../../breakpoints/breakpoint.service';
import { HeaderPublicVm } from '../header-public.viewmodel';
import { HeaderRowMainComponent } from '../row-main/header-row-main.component';
import { HeaderRowSecondaryComponent } from '../row-secondary/header-row-secondary.component';

@Component({
  selector: 'tchl-public-header',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatIconModule,
    HeaderRowSecondaryComponent,
    HeaderRowMainComponent,
  ],
  templateUrl: './public-header.component.html',
  styleUrl: './public-header.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicHeaderComponent {
  // inputs depuis pageModel.header.properties.* côté public
  properties = input.required<HeaderProperties>();

  flags = input<string[]>([]); // feature flags on/off pour filtrer nav etc.
  // outputs
  menuClick = output<void>(); // burger -> shell ouvre overlay
  changeLang = output<string>();
  toggleTheme = output<ThemeMode>();
  searchClick = output<void>();
  currentPath = signal('/');
  private readonly breakpoint = inject(TchBreakpointService);
  handset = this.breakpoint.handset; // boolean signal
  tablet = this.breakpoint.tablet;
  desktop = this.breakpoint.desktop;
  private readonly router = inject(Router);
  private readonly i18n = inject(I18nFacade);
  // i18n/theme
  currentLang = this.i18n.current;
  availableLangs = this.i18n.available;
  private readonly theme = inject(ThemeService);
  themeMode = this.theme.mode;
  private readonly overlay = inject(OverlayService);
  private readonly analytics = inject(AnalyticsService);

  /* ------------------------------------------------------------------
   * Derived UI bits
   * ------------------------------------------------------------------ */
  private readonly features = inject(FeatureService);
  // feature flags globals
  private readonly DEFAULT_FLAG_VALUE = environment.feature.defaultValue;

  /** Check feature flag / toggle visibility for nav items or actions */
  featureOn = (flag?: string | null): boolean => {
    if (!flag) return true;
    try {
      const enabled = this.features.isEnabled(flag);
      return enabled || this.DEFAULT_FLAG_VALUE; // fallback
    } catch {
      return this.DEFAULT_FLAG_VALUE;
    }
  };

  filteredNav = computed(() => {
    return (this.properties()?.navigation ?? []).filter(link => this.featureOn(link.flag));
  });
  ctaLink = computed(() => {
    const cta = this.properties()?.cta?.public;
    if (!cta) return null;
    if (!this.featureOn(cta.flag)) return null;
    return cta;
  });
  vm = computed(() => {
    const handset = this.handset();
    const tablet = this.tablet();
    const desktop = this.desktop();

    const props = this.properties();
    const cta = this.ctaLink();

    // search/lang/theme availability
    const showSearch = this.featureOn(props?.actions?.search?.flag);
    const showLang = this.featureOn(props?.actions?.lang?.flag);
    const showTheme = this.featureOn(props?.actions?.theme?.flag);
    const showLangTheme = showLang || showTheme;

    return {
      // responsive state
      handset,
      tablet,
      desktop,

      // brand
      brand: props?.brand,
      onBrandClick: () => this.onBrandHome(),

      // CTA
      ctaLabel: cta?.labelKey,
      ctaPath: cta?.path,
      showCtaInMainRow: !handset,

      // features
      showSearch,
      showLangTheme,

      // lang/theme state
      langState: {
        currentLang: this.currentLang(),
        availableLangs: this.availableLangs(),
      },
      themeMode: this.themeMode(),

      // account
      isAuthenticated: false, // à gérer avec auth service
      accountAriaLabel: props?.account?.public?.labelKey,
      onAccountClick: () => this.onAccountClick(),

      // nav
      navItems: this.filteredNav(),
      currentPath: this.currentPath(),

      // burger (mobile only)
      showBurger: handset,
      onBurgerClick: () => this.onBurgerClick(),

      // actions
      onSearchClick: () => this.onSearchClick(),
      onChangeLang: (lang: string) => this.onChangeLang(lang),
      onToggleTheme: (mode: string) => this.onToggleTheme(mode as ThemeMode),
      onNavSelect: (path: string) => this.go(path),
    } satisfies HeaderPublicVm;
  });

  // actions
  onBrandHome() {
    this.currentPath.set('/');
    this.router.navigateByUrl('/');
    try {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch {
      console.error('error displaying home');
    }
  }

  go(path: string | undefined) {
    if (!path) return;
    this.currentPath.set(path);
    this.router.navigateByUrl(path);
  }

  onBurgerClick() {
    this.menuClick.emit(); // Shell gère overlay
  }

  onSearchClick() {
    this.searchClick.emit();
    document.documentElement.classList.add('search-open');
    this.overlay.show();
    this.analytics.pageView('open_search_from_button_public');
  }

  onChangeLang(lang: string) {
    this.i18n.setCurrent(lang);
    this.changeLang.emit(lang);
  }

  onToggleTheme(mode: ThemeMode) {
    this.theme.setMode(mode);
    this.toggleTheme.emit(mode);
  }

  onAccountClick() {
    const path = this.properties().account?.public?.path;
    if (path) {
      this.router.navigateByUrl(path);
    }
  }
}
