import { ConfigurableFocusTrapFactory, FocusTrap } from '@angular/cdk/a11y';
import { DOCUMENT } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  computed,
  effect,
  inject,
  input,
  model,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import {
  ActionItem,
  NavigationSection,
  actionHref,
  actionQueryParams,
  actionRoute,
  actionText,
  isExternalAction,
  isRouteAction,
} from '@tch/api';
import { PrivateBootstrapStore } from '@tch/core/auth';
import { I18nFacade } from '@tch/core/i18n';
import { RuntimeSettingsStore } from '@tch/shared-config';
import { PrivateNotificationBellComponent } from '@tch/notifications';
import {
  TchBrand,
  TchBreakpointService,
  TchDrawerNav,
  TchSidebarNav,
  TchUserMenu,
} from '@tch/ui/components';
import { ThemeMode, ThemeStore } from '@tch/ui/theme';

import { ShellFeedbackOutletComponent } from '../feedback/shell-feedback-outlet.component';
import { ShellFeedbackVerbosity } from '../feedback/shell-feedback.model';

interface ShellLanguageOption {
  readonly id: string;
  readonly label: string;
  readonly shortLabel: string;
}

const SHELL_LANGUAGES: readonly ShellLanguageOption[] = [
  { id: 'ht', label: 'Kreyòl', shortLabel: 'HT' },
  { id: 'fr', label: 'Français', shortLabel: 'FR' },
  { id: 'en', label: 'English', shortLabel: 'EN' },
];

const SHELL_THEME_MODES: readonly ThemeMode[] = ['system', 'light', 'dark'];
type ShellSettingsPanel = 'language' | 'theme';

/**
 * Shell des consoles privées.
 *
 * Sous `expanded` (840px) le drawer est un **dialogue modal** : focus piégé, contenu `inert`,
 * scroll verrouillé, `Escape` ferme, focus rendu au burger. Au-dessus, c'est une sidebar
 * permanente sans sémantique de dialogue. Voir `docs/conventions/style.md` §10.1.
 */
@Component({
  selector: 'tch-private-shell-layout',
  imports: [
    ShellFeedbackOutletComponent,
    PrivateNotificationBellComponent,
    TchBrand,
    TchDrawerNav,
    TchSidebarNav,
    TchUserMenu,
    RouterLink,
    TranslatePipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'onEscape()' },
  templateUrl: './private-shell-layout.component.html',
  styleUrl: './private-shell-layout.component.scss',
})
export class PrivateShellLayoutComponent {
  private readonly document = inject(DOCUMENT);
  private readonly focusTrapFactory = inject(ConfigurableFocusTrapFactory);
  private readonly breakpoints = inject(TchBreakpointService);
  private readonly bootstrap = inject(PrivateBootstrapStore);
  protected readonly theme = inject(ThemeStore);
  protected readonly i18n = inject(I18nFacade);
  private readonly runtimeSettings = inject(RuntimeSettingsStore);

  readonly brand = input.required<ActionItem>();
  readonly titleKey = input('');
  readonly primary = input<readonly ActionItem[]>([]);
  readonly sections = input<readonly NavigationSection[]>([]);
  readonly secondary = input<readonly ActionItem[]>([]);
  readonly userName = input('');
  readonly darkMode = input(false);
  readonly feedbackVerbosity = input<ShellFeedbackVerbosity>('standard');
  readonly drawerOpen = model(false);

  readonly themeToggled = output<void>();
  readonly profileRequested = output<void>();
  readonly logoutRequested = output<void>();

  readonly actionRoute = actionRoute;
  readonly actionHref = actionHref;
  readonly actionQueryParams = actionQueryParams;
  readonly actionText = actionText;
  readonly isExternalAction = isExternalAction;
  readonly isRouteAction = isRouteAction;

  readonly themeIcon = computed(() => (this.darkMode() ? 'light_mode' : 'dark_mode'));
  readonly settingsPanel = signal<ShellSettingsPanel | null>(null);
  readonly drawerTenantName = computed(() => {
    // 1. Nom renseigné par la page dashboard (applyTenantDashboardSettings)
    const fromSettings = this.runtimeSettings.settings().values['tenant.displayName'];
    if (typeof fromSettings === 'string' && fromSettings.trim()) {
      return fromSettings.trim();
    }
    // 2. Fallback : contexte du bootstrap (/runtime/private)
    const ctx = this.bootstrap.tenantContext();
    return (ctx?.tenantName?.trim() || ctx?.tenantCode?.trim() || '').trim();
  });
  /** Afficher l'étiquette "Plateforme" uniquement quand l'espace est explicitement PLATFORM.
   * En ADMIN pré-bootstrap (space = null) on préfère le blanc à "Plateforme" trompeur. */
  readonly drawerShowPlatformLabel = computed(() => this.bootstrap.space() === 'PLATFORM');
  readonly drawerScopeKey = computed(() => 'surface.tenant_admin');
  /** Nom affiché dans la barre desktop : même logique que le drawer mobile. */
  readonly topBarContextName = computed(() => {
    if (this.drawerTenantName()) return this.drawerTenantName();
    if (this.drawerShowPlatformLabel()) return null; // affiché via topBarScopeLabelKey
    return null;
  });
  /** Clé de scope pour la barre desktop : tient compte de l'espace courant. */
  readonly topBarScopeLabelKey = computed(() =>
    this.bootstrap.space() === 'PLATFORM' ? 'surface.platform_admin' : 'surface.tenant_admin',
  );
  readonly drawerSecondary = computed(() => this.secondary().filter(item => item.id !== 'help'));
  readonly helpAction = computed(() => this.secondary().find(item => item.id === 'help') ?? null);
  readonly availableLanguages = computed(() => {
    const supported = new Set(this.i18n.languages());
    const configured = SHELL_LANGUAGES.filter(language => supported.has(language.id));
    return configured.length > 0 ? configured : SHELL_LANGUAGES;
  });
  readonly currentLanguage = computed(() => {
    const current = this.i18n.currentLanguage();
    return (
      this.availableLanguages().find(language => language.id === current) ??
      this.availableLanguages()[0] ??
      SHELL_LANGUAGES[0]
    );
  });
  readonly themeModes = SHELL_THEME_MODES;
  readonly currentThemeMode = computed(() => this.theme.activeTheme().mode);
  readonly currentThemeLabelKey = computed(() => `theme.modes.${this.currentThemeMode()}`);

  /** Sous 840px le drawer se superpose au contenu ; au-dessus il est permanent. */
  readonly overlayMode = computed(() => !this.breakpoints.isWide());
  /** Drawer superposé et ouvert : la sémantique de dialogue s'applique. */
  readonly drawerModal = computed(() => this.overlayMode() && this.drawerOpen());
  /** Drawer superposé et fermé : hors écran, donc retiré du parcours clavier. */
  readonly drawerHidden = computed(() => this.overlayMode() && !this.drawerOpen());

  private readonly drawerRef = viewChild<ElementRef<HTMLElement>>('drawer');
  private readonly contentRef = viewChild<ElementRef<HTMLElement>>('content');
  private readonly drawerNav = viewChild(TchDrawerNav);
  private focusTrap: FocusTrap | null = null;
  private trigger: HTMLElement | null = null;

  constructor() {
    effect(() => {
      const modal = this.drawerModal();
      const drawer = this.drawerRef()?.nativeElement;
      if (!drawer) return;

      // `ConfigurableFocusTrapFactory.create()` insère deux sentinelles comme enfants directs de
      // `.workspace`, juste avant et après `.drawer`. En sidebar permanente (2 colonnes), ce sont
      // deux items de plus dans la grille : l'auto-placement pousse le drawer réel en colonne 2 et
      // le contenu à la ligne suivante, empilés au lieu d'être côte à côte — corrigé en détruisant
      // le piège dès qu'on repasse en sidebar permanente.
      //
      // Le même défaut réapparaît en overlay (1 colonne) tant que le drawer est FERMÉ : ces
      // sentinelles sont censées être invisibles (`cdk-visually-hidden`), mais dans cette grille
      // elles rendent avec une vraie hauteur (mesuré ~120px chacune sur staging à 768px) — deux
      // lignes vides de ~240px au total avant `<main>`. Le piège ne sert que pendant que le drawer
      // est réellement ouvert (`modal`) ; il n'y a donc aucune raison de le garder vivant le reste
      // du temps, que ce soit en sidebar permanente ou en overlay fermé.
      if (modal) {
        this.focusTrap ??= this.focusTrapFactory.create(drawer);
        this.focusTrap.enabled = true;
      } else {
        this.focusTrap?.destroy();
        this.focusTrap = null;
      }

      this.document.documentElement.classList.toggle('tch-overlay-open', modal);
      this.document.body.classList.toggle('tch-overlay-open', modal);
      this.contentRef()?.nativeElement.toggleAttribute('inert', modal);

      if (modal && !this.trigger) {
        // Le bloc précédent vient de créer `this.focusTrap` dès que `modal` est vrai : il existe
        // forcément ici, TS ne le sait pas.
        this.trigger = this.document.activeElement as HTMLElement | null;
        void this.focusTrap!.focusInitialElementWhenReady();
      } else if (!modal && this.trigger) {
        const trigger = this.trigger;
        this.trigger = null;
        trigger.focus();
      }
    });

    inject(DestroyRef).onDestroy(() => {
      this.focusTrap?.destroy();
      this.focusTrap = null;
      this.document.documentElement.classList.remove('tch-overlay-open');
      this.document.body.classList.remove('tch-overlay-open');
    });
  }

  toggleDrawer(): void {
    this.drawerOpen.update(open => !open);
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
    // Rouvrir le menu doit repartir de la racine, pas de la catégorie consultée la fois d'avant.
    // On remet l'état à plat sans passer par `closeCategoryPanel()`, qui rendrait le focus à une
    // carte sur le point d'être masquée.
    this.drawerNav()?.openCategoryId.set(null);
  }

  toggleSettingsPanel(panel: ShellSettingsPanel): void {
    this.settingsPanel.update(open => (open === panel ? null : panel));
  }

  setLanguage(language: string): void {
    this.i18n.setCurrent(language);
    this.settingsPanel.set(null);
  }

  setThemeMode(mode: ThemeMode): void {
    this.theme.setMode(mode);
    this.settingsPanel.set(null);
  }

  /** Escape referme un niveau à la fois : d'abord le panneau de catégorie, puis le drawer. */
  onEscape(): void {
    if (this.settingsPanel()) {
      this.settingsPanel.set(null);
      return;
    }

    if (!this.drawerModal()) return;

    const nav = this.drawerNav();
    if (nav?.openCategoryId()) {
      nav.closeCategoryPanel();
      return;
    }
    this.closeDrawer();
  }
}
