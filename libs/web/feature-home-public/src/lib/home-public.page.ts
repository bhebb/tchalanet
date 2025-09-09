import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FooterModel, HeaderModel } from 'shared/types';

@Component({
  selector: 'lib-home-public-page',
  standalone: true,
  imports: [],
  template: `

    <!-- Ici tu rends tes sections (hero/news/partners) via ton moteur config-driven -->
    <h1>Accueil public</h1>
    <p>Home page affiche</p>

  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomePublicPage {
  header: HeaderModel = {
    logoUrl: '/assets/brand/logo.svg',
    menu: [{ id: 'home', label: 'Accueil', route: '/' }, { id: 'results', label: 'Résultats', route: '/results' }],
    cta: { label: 'Se connecter', href: '/login' },
    langs: ['fr', 'en'],
    currentLang: 'fr'
  };
  footer: FooterModel = {
    columns: [
      { title: 'À propos', links: [{ label: 'Entreprise', href: '/company' }] },
      { title: 'Aide', links: [{ label: 'Support', href: '/support' }] },
      { title: 'Légal', links: [{ label: 'Confidentialité', href: '/privacy' }] }
    ],
    note: '© 2025 Tchalanet'
  };

  onLang(l: string) {
  }
}
