import { DOCUMENT } from '@angular/common';
import { Inject, Injectable } from '@angular/core';
import { OverlayContainer } from '@angular/cdk/overlay';
import { TchTheme } from '@tchl/types';

const STYLE_TAG_ID = 'tch-theme-vars';
const THEME_ROOT_ID = 'theme-root';           // <div id="theme-root" class="tch-theme">
const THEME_ROOT_FALLBACK = '.tch-theme';     // classe générique de conteneur

@Injectable({ providedIn: 'root' })
export class ThemeDomApplier {
  constructor(private overlay: OverlayContainer, @Inject(DOCUMENT) private doc: Document) {}

  apply(theme: TchTheme, effMode: 'light' | 'dark', density: 0 | -1 | -2) {
    this.applyMaterialClass(theme.matClass);
    this.applyDensity(density);

    // 1) Toggle du mode sombre là où le SCSS l’attend (root + overlay)
    const root = this.findThemeRoot();
    root.classList.toggle('dark', effMode === 'dark');
    this.overlay.getContainerElement().classList.toggle('dark', effMode === 'dark');

    // 2) Ecrire les variables CSS (light + dark)
    this.writeCssVars(theme);

    // 3) (option) indiquer le mode sur <html> si d’autres styles s’en servent
    this.doc.documentElement.setAttribute('data-theme', effMode);
  }

  // ---------- Privé ----------

  private findThemeRoot(): HTMLElement {
    return (
      (this.doc.getElementById(THEME_ROOT_ID) as HTMLElement) ||
      (this.doc.querySelector(THEME_ROOT_FALLBACK) as HTMLElement) ||
      this.doc.body
    );
  }

  private applyMaterialClass(matClass?: string) {
    const body = this.doc.body.classList;
    const overlay = this.overlay.getContainerElement().classList;
    // Nettoie les anciennes classes Material éventuelles
    Array.from(body).filter(c => c.startsWith('mat-')).forEach(c => body.remove(c));
    Array.from(overlay).filter(c => c.startsWith('mat-')).forEach(c => overlay.remove(c));
    // Applique la classe Material du preset si fournie
    if (matClass) {
      body.add(matClass);
      overlay.add(matClass);
    }
  }

  private applyDensity(d: 0 | -1 | -2) {
    const cl = this.doc.documentElement.classList;
    ['mat-density-0', 'mat-density-1', 'mat-density-2'].forEach(c => cl.remove(c));
    cl.add(`mat-density-${d === 0 ? '0' : d === -1 ? '1' : '2'}`);
  }

  private writeCssVars(theme: TchTheme) {
    const { lightVars, darkVars } = this.composeCssVars(theme);

    const css =
      `:root{\n` +
      Object.entries(lightVars).map(([k, v]) => `  ${k}:${v};`).join('\n') +
      `\n}\n` +
      `:root[data-theme="dark"]{\n` +
      Object.entries(darkVars).map(([k, v]) => `  ${k}:${v};`).join('\n') +
      `\n}`;

    let tag = this.doc.getElementById(STYLE_TAG_ID) as HTMLStyleElement | null;
    if (!tag) {
      tag = this.doc.createElement('style');
      tag.id = STYLE_TAG_ID;
      this.doc.head.appendChild(tag);
    }
    tag.innerHTML = css;
  }

  private composeCssVars(theme: TchTheme): {
    lightVars: Record<string, string>;
    darkVars: Record<string, string>;
  } {
    const p = theme.palette ?? {};
    const t = theme.tokens ?? {};
    const outLight: Record<string, string> = {};
    const outDark: Record<string, string> = {};

    // ---- Material 3 (runtime) ----
    outLight['--mat-sys-color-primary']      = p.primary ?? '#134D9F';
    outLight['--mat-sys-color-on-primary']   = p.onPrimary ?? '#ffffff';
    outLight['--mat-sys-color-tertiary']     = p.tertiary ?? p.accent ?? '#D84C51';
    outLight['--mat-sys-color-on-tertiary']  = p.onTertiary ?? '#ffffff';
    outLight['--mat-sys-color-surface']      = p.surface ?? '#ffffff';
    outLight['--mat-sys-color-on-surface']   = p.onSurface ?? '#111111';
    outLight['--mat-sys-color-outline']      = p.outline ?? 'rgba(0,0,0,.16)';

    // ---- Bridge global (fond/texte document) ----
    outLight['--tch-color-primary']    = 'var(--mat-sys-color-primary)';
    outLight['--tch-color-surface']    = 'var(--mat-sys-color-surface)';
    outLight['--tch-color-on-surface'] = 'var(--mat-sys-color-on-surface)';

    // Accent/pastille
    outLight['--accent-dot']           = p.accent ?? '#D84C51';
    outLight['--accent-dot-contrast']  = '#fff';

    // Tokens optionnels imposés par le preset (on respecte s’ils existent)
    if (t.headerBg) outLight['--header-bg'] = t.headerBg;
    if (t.headerFg) outLight['--header-fg'] = t.headerFg;
    if (t.footerBg) outLight['--footer-bg'] = t.footerBg;
    if (t.footerFg) outLight['--footer-fg'] = t.footerFg;

    // Optionnel : autres tokens palette
    if (p.surfaceContainer) outLight['--tch-surface-container'] = p.surfaceContainer;
    if (p.shape?.cornerRadius != null) outLight['--tch-shape-radius'] = `${p.shape.cornerRadius}px`;

    if (theme.typography?.family) {
      outLight['--tch-font-family'] = theme.typography.family;
    }
    if (theme.typography?.weights) {
      const w = theme.typography.weights;
      outLight['--tch-w-regular']  = String(w.regular ?? 400);
      outLight['--tch-w-medium']   = String(w.medium  ?? 500);
      outLight['--tch-w-semibold'] = String(w.semibold?? 600);
      outLight['--tch-w-bold']     = String(w.bold    ?? 700);
    }
    if (theme.typography?.scale) {
      outLight['--tch-type-scale'] = String(theme.typography.scale); // ex: 0.95, 1.0, 1.05
    }
    this.ensureFontLoaded(theme.typography?.loadHref)

    // ---- DARK overrides (document) ----
    // On laisse le calcul des surfaces header/footer au SCSS.
    outDark['--tch-color-surface']    = 'color-mix(in oklab, #000 92%, var(--mat-sys-color-primary) 8%)';
    outDark['--tch-color-on-surface'] = '#e6edf7';
    // Outline plus discret en dark
    outDark['--mat-sys-color-outline'] = 'color-mix(in oklab, #fff 12%, transparent)';

    // ---- Overrides cssVars du preset ----
    // - clés normales → light
    // - clés préfixées "--dark:" → dark (clé réelle = sans le préfixe)
    if (theme.cssVars) {
      for (const [k, v] of Object.entries(theme.cssVars)) {
        if (k.startsWith('--dark:')) {
          outDark[k.slice(7)] = v;
        } else {
          outLight[k] = v;
        }
      }
    }

    return { lightVars: outLight, darkVars: outDark };
  }

  private ensureFontLoaded(href?: string) {
    if (!href) return;
    const id = 'tch-theme-font';
    if (this.doc.getElementById(id)) return;
    const link = this.doc.createElement('link');
    link.id = id; link.rel = 'stylesheet'; link.href = href;
    this.doc.head.appendChild(link);
  }
}
