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
  viewChild,
} from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { ActionItem, NavigationSection } from '@tch/api';
import { PrivateNotificationBellComponent } from '@tch/notifications';
import {
  TchBrand,
  TchBreakpointService,
  TchDrawerNav,
  TchSidebarNav,
  TchUserMenu,
} from '@tch/ui/components';

import { ShellFeedbackOutletComponent } from '../feedback/shell-feedback-outlet.component';
import { ShellFeedbackVerbosity } from '../feedback/shell-feedback.model';

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

  readonly themeIcon = computed(() => (this.darkMode() ? 'light_mode' : 'dark_mode'));

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
      const wide = this.breakpoints.isWide();
      const drawer = this.drawerRef()?.nativeElement;
      if (!drawer) return;

      // `ConfigurableFocusTrapFactory.create()` insère deux sentinelles invisibles comme enfants
      // directs de `.workspace`, juste avant et après `.drawer`. En sidebar permanente ce sont
      // deux items de plus dans une grille à 2 colonnes : l'auto-placement pousse le drawer réel
      // en colonne 2 et le contenu à la ligne suivante, empilés au lieu d'être côte à côte. Le
      // piège ne sert qu'en overlay, donc il est détruit — pas seulement désactivé — dès qu'on
      // repasse en sidebar permanente.
      if (wide) {
        this.focusTrap?.destroy();
        this.focusTrap = null;
      } else {
        this.focusTrap ??= this.focusTrapFactory.create(drawer);
        this.focusTrap.enabled = modal;
      }

      this.document.documentElement.classList.toggle('tch-overlay-open', modal);
      this.document.body.classList.toggle('tch-overlay-open', modal);
      this.contentRef()?.nativeElement.toggleAttribute('inert', modal);

      if (modal && !this.trigger) {
        // `modal` implique `!wide` (`drawerModal` = `overlayMode() && drawerOpen()`, et
        // `overlayMode()` = `!isWide()`) : le piège existe forcément ici, TS ne le sait pas.
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

  /** Escape referme un niveau à la fois : d'abord le panneau de catégorie, puis le drawer. */
  onEscape(): void {
    if (!this.drawerModal()) return;

    const nav = this.drawerNav();
    if (nav?.openCategoryId()) {
      nav.closeCategoryPanel();
      return;
    }
    this.closeDrawer();
  }
}
