import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { IsActiveMatchOptions, Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { Brand, TchLink, ThemeMode } from '@tchl/types';
import { I18nFacade } from '@tchl/facades';
import { BreakpointObserver } from '@angular/cdk/layout';
import { TranslatePipe } from '@ngx-translate/core';
import { BrandComponent } from '../brand/brand.component';
import { LangThemeGroupComponent } from '../lang-theme-group/lang-theme-group.component';
import { NavComponent } from '@tchl/web/widgets';
import { ThemeService } from '@tchl/ui/theme';
import { OverlayService } from '@tchl/search';
import { AnalyticsService } from '@tchl/analytics';
import { MQ } from '../breakpoints/breakpoints';
import { distinctUntilChanged, map } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatDrawer, MatSidenavModule } from '@angular/material/sidenav';

@Component({
  selector: 'tchl-header',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatIconModule,
    MatMenuModule,
    MatButtonModule,
    MatSidenavModule,
    TranslatePipe,
    BrandComponent,
    LangThemeGroupComponent,
    NavComponent,
  ],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'tchl-header-host' },
})
export class HeaderComponent {
  private bp = inject(BreakpointObserver);
  private router = inject(Router);
  themeService = inject(ThemeService);
  i18n = inject(I18nFacade);
  overlay = inject(OverlayService);
  analyticsService = inject(AnalyticsService);

  private exact: IsActiveMatchOptions = {
    paths: 'exact',
    queryParams: 'ignored',
    fragment: 'ignored',
    matrixParams: 'ignored',
  };
  private startsWith: IsActiveMatchOptions = {
    paths: 'subset',
    queryParams: 'ignored',
    fragment: 'ignored',
    matrixParams: 'ignored',
  };

  @ViewChild('mobileNav', { static: false }) mobileNav?: MatDrawer;

  // Inputs
  mode = input<'public' | 'private'>('public');
  brand = input<Brand | undefined>();
  navigation = input<TchLink[]>([]);
  cta = input<TchLink>();
  user = input<{ name?: string; avatarUrl?: string } | null>(null);

  // Flags d’affichage
  showLang = input(true);
  showTheme = input(true);
  showSearch = input(false);

  // Sorties (si utilisées par le parent)
  toggleTheme = output<void>();
  changeLang = output<string>();
  login = output<void>();
  register = output<void>();
  profile = output<void>();
  logout = output<void>();
  searchQuery = output<string>();
  setTheme = output<string>();

  // État & dérivées
  theme = signal<ThemeMode>('light');
  isPrivate = computed(() => this.mode() === 'private');

  private _handset = toSignal(
    this.bp.observe(MQ.HANDSET).pipe(
      map(r => r.matches),
      distinctUntilChanged(),
    ),
    { initialValue: false },
  );
  private _tablet = toSignal(
    this.bp.observe(MQ.TABLET).pipe(
      map(r => r.matches),
      distinctUntilChanged(),
    ),
    { initialValue: false },
  );
  private _desktop = toSignal(
    this.bp.observe(MQ.DESKTOP).pipe(
      map(r => r.matches),
      distinctUntilChanged(),
    ),
    { initialValue: false },
  );

  readonly handset = computed(() => this._handset());
  readonly tablet = computed(() => this._tablet());
  readonly isDesktop = computed(() => this._desktop());

  // i18n / thème (signals)
  availableLangs = this.i18n.available;
  currentLang = this.i18n.current;
  themeMode = this.themeService.mode;

  currentPath = '/';

  constructor() {
    effect(() => {
      const mode = this.themeMode();
      try {
        localStorage.setItem('tch.theme.mode', mode);
      } catch {}
    });
  }

  /* ---------- Actions UI ---------- */

  setLang(lang: string) {
    this.i18n.setCurrent(lang);
  }

  setThemeMode = (mode: ThemeMode) => this.themeService.setMode(mode);

  onBrandHome() {
    this.currentPath = '/';
    this.router.navigateByUrl('/');
    try {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch {}
  }

  go(path: any) {
    const current: string = path?.value ?? (path as string);
    this.currentPath = current;
    this.router.navigate([current]);
  }

  isActive(path: string): boolean {
    if (!path) return false;
    const tree = this.router.createUrlTree([path]);
    const opts = path === '/' ? this.exact : this.startsWith;
    return this.router.isActive(tree, opts);
  }

  onSearchClick() {
    document.documentElement.classList.add('search-open');
    this.overlay.show();
    this.analyticsService.pageView('open_search_from_button');
  }

  onDrawerOpenedChange(opened: boolean) {
    const cls = 'no-scroll';
    document.documentElement.classList.toggle(cls, opened);
    document.body.classList.toggle(cls, opened);
  }

  openSidenav() {
    this.mobileNav?.open();
  }
  closeSidenav() {
    this.mobileNav?.close();
  }

  onAvatarClick() {
    if (this.user()) {
      this.router.navigateByUrl('/app/profile');
    } else {
      this.router.navigateByUrl('/login');
    }
  }
}
