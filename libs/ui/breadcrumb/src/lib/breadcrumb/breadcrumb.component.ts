// breadcrumb.component.ts
import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  ActivatedRoute,
  ActivatedRouteSnapshot,
  NavigationEnd,
  Params,
  PRIMARY_OUTLET,
  Router,
  RouterLink,
} from '@angular/router';
import { filter } from 'rxjs/operators';
import { I18nFacade } from '@tchl/facades';

export type Crumb = { label: string; url?: string; aria?: string };

type BreadInput =
  | string
  | false
  | ((ctx: {
      data: any;
      params: Params;
      query: Params;
      url: string;
      route: ActivatedRouteSnapshot;
    }) => string);

@Component({
  selector: 'tchl-breadcrumb',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    @if (crumbs(); as crumbs) { @if (crumbs.length > 1) {
    <div class="tch-breadcrumb">
      <nav aria-label="Breadcrumb">
        <ol>
          @for (c of crumbs; track c.url; ) {
          <li>
            @if (!$last && c.url) {
            <a [routerLink]="c.url" [attr.aria-label]="c.aria || c.label">{{ c.label }}</a>
            } @else {
            <span class="current" [attr.aria-current]="'page'">{{ c.label }}</span>
            }
          </li>
          }
        </ol>
      </nav>
    </div>
    } }
  `,
  styleUrls: ['./breadcrumb.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BreadcrumbComponent {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private i18n = inject(I18nFacade);

  readonly crumbs = signal<Crumb[]>([]);

  constructor() {
    this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe(() => {
      this.crumbs.set(this.buildCrumbs());
    });
    // première passe
    Promise.resolve().then(() => this.crumbs.set(this.buildCrumbs()));
  }

  private buildCrumbs(): Crumb[] {
    const trail: Crumb[] = [];
    const query = this.route.snapshot.queryParams;

    let node: ActivatedRouteSnapshot | null = this.route.root.snapshot;
    let acc: string[] = [];

    while (node) {
      // on ne garde que la voie primaire
      if (node.outlet !== PRIMARY_OUTLET) {
        node = node.firstChild ?? null;
        continue;
      }

      // Segments RÉELS (avec :id déjà remplacés)
      const segs = node.url.map(s => s.path).filter(Boolean);
      if (segs.length) acc.push(...segs);

      const url = '/' + acc.join('/'); // URL cumulée jusqu’ici

      const raw: BreadInput | undefined = node.data?.['breadcrumb'] ?? node.data?.['titleKey'];
      const label = this.resolveLabel(raw, node, query, url);

      // on pousse un maillon si on a un label et si pas explicitement masqué
      if (label) {
        trail.push({
          label,
          url: node.firstChild ? url : undefined, // le dernier n’est pas cliquable
        });
      }

      node = node.firstChild ?? null;
    }

    // Préfixe "Accueil" si absent
    const home = this.i18n.instant('nav.home') || 'Accueil';
    if (!trail.length || trail[0].label.toLowerCase() !== home.toLowerCase()) {
      trail.unshift({ label: home, url: '/' });
    }

    return trail;
  }

  private resolveLabel(
    raw: BreadInput | undefined,
    route: ActivatedRouteSnapshot,
    query: Params,
    url: string,
  ): string | null {
    if (raw === false) return null;

    // fonction → on lui passe le contexte
    if (typeof raw === 'function') {
      try {
        const res = raw({ data: route.data, params: route.params, query, url, route });
        return this.take(res);
      } catch {
        /* ignore */
      }
    }

    // string → i18n.instant si possible, sinon brut
    if (typeof raw === 'string' && raw.trim()) {
      const t = this.i18n.instant(raw);
      return this.take(t || raw);
    }

    // fallback : préférer un titre venant d’un resolver (data.title/name)
    const dataTitle = (route.data?.['title'] || route.data?.['name'] || route.data?.['label']) as
      | string
      | undefined;
    if (dataTitle) return this.take(dataTitle);

    // sinon un param connu (slug/id/code)
    const p = route.params || {};
    const paramGuess = p['title'] || p['name'] || p['slug'] || p['id'] || p['code'];
    if (paramGuess) return this.take(String(paramGuess));

    // dernière chance : le dernier segment littéral (ex: 'settings')
    const seg = route.url?.[route.url.length - 1]?.path;
    if (seg) return this.take(this.humanize(seg));

    return null;
  }

  private humanize(seg: string): string {
    return seg.replace(/[-_]/g, ' ').replace(/\b\w/g, m => m.toUpperCase());
  }
  private take(s: string | undefined): string | null {
    const v = (s ?? '').trim();
    return v ? v : null;
  }
}
