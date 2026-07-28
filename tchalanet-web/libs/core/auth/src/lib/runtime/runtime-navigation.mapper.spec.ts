import { describe, expect, it } from 'vitest';

import { RuntimeNavigationDrawer } from './private-bootstrap.model';
import { footerFromRuntimeNavigation, sectionsFromRuntimeNavigation } from './runtime-navigation.mapper';

/**
 * `PrivateShellNavigationResolver` (backend) bypasses the `NavigationDrawer` Java record — it
 * passes the on-disk JSON fragment straight through untyped. That fragment names its top-level
 * fields `primary`/`secondary`, not the record's `topDestinations`/`footerDestinations`. Reading
 * only the record's names — which `footerFromRuntimeNavigation` did until now — silently returns
 * nothing on every real request; the drawer footer then always falls back to the static model
 * without anyone noticing, because the fallback looks like a reasonable menu on its own.
 *
 * These tests pin the real field name (`secondary`) as the primary source, with the record's name
 * kept as a forward-compatible fallback.
 */
describe('footerFromRuntimeNavigation', () => {
  it('reads `secondary` — the field name the real backend resolver actually serves', () => {
    const drawer: RuntimeNavigationDrawer = {
      secondary: [
        { id: 'help', labelKey: 'nav.help', path: '/help', kind: 'internal' },
      ],
    };

    const footer = footerFromRuntimeNavigation(drawer);

    expect(footer?.map(item => item.id)).toEqual(['help']);
  });

  it('falls back to `footerDestinations` if a backend is ever wired to the Java record instead', () => {
    const drawer: RuntimeNavigationDrawer = {
      footerDestinations: [
        { id: 'help', labelKey: 'nav.help', path: '/help', kind: 'internal' },
      ],
    };

    const footer = footerFromRuntimeNavigation(drawer);

    expect(footer?.map(item => item.id)).toEqual(['help']);
  });

  it('prefers `secondary` when both are present, since that is what is real today', () => {
    const drawer: RuntimeNavigationDrawer = {
      secondary: [{ id: 'real', labelKey: 'nav.real', path: '/real', kind: 'internal' }],
      footerDestinations: [{ id: 'stale', labelKey: 'nav.stale', path: '/stale', kind: 'internal' }],
    };

    const footer = footerFromRuntimeNavigation(drawer);

    expect(footer?.map(item => item.id)).toEqual(['real']);
  });

  it('returns null — not an empty array — when neither field is present', () => {
    expect(footerFromRuntimeNavigation({})).toBeNull();
    expect(footerFromRuntimeNavigation(null)).toBeNull();
  });
});

describe('sectionsFromRuntimeNavigation', () => {
  it('maps each backend section, in order, to a NavigationSection', () => {
    const drawer: RuntimeNavigationDrawer = {
      sections: [
        {
          id: 'admin',
          labelKey: 'nav.admin.section.admin',
          items: [{ id: 'dashboard', labelKey: 'nav.dashboard', path: '/app/admin', kind: 'internal' }],
        },
        {
          id: 'config',
          labelKey: 'nav.admin.section.config',
          items: [{ id: 'setup', labelKey: 'nav.setup', path: '/app/admin/setup', kind: 'internal' }],
        },
      ],
    };

    const sections = sectionsFromRuntimeNavigation(drawer);

    expect(sections?.map(section => section.id)).toEqual(['admin', 'config']);
    expect(sections?.map(section => section.items.map(item => item.id))).toEqual([
      ['dashboard'],
      ['setup'],
    ]);
  });

  it('returns null when the backend sends no sections, so the static fallback applies', () => {
    expect(sectionsFromRuntimeNavigation({ sections: [] })).toBeNull();
    expect(sectionsFromRuntimeNavigation(null)).toBeNull();
  });
});
