import { createGlobPatternsForDependencies } from '@nx/angular/tailwind';
import daisyui from 'daisyui';
import { join } from 'path';

export default { // 👈 CHANGEMENT N°2 : On utilise 'export default'
  content: [
    join(__dirname, 'src/**/!(*.stories|*.spec).{ts,html}'),
    ...createGlobPatternsForDependencies(__dirname)
  ],
  theme: {
    extend: {}
  },
  plugins: [daisyui],
  daisyui: {
    themes: ['light', 'dark', 'corporate', {
      'tchalanet': {
        'primary': '#6419E6',
        'secondary': '#D926A9',
        'accent': '#1FB2A6',
        'neutral': '#191D24',
        'base-100': '#2A303C',
        'info': '#3ABFF8',
        'success': '#36D399',
        'warning': '#FBBD23',
        'error': '#F87272'
      }
    }]
  }
};
