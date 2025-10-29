module.exports = {
  extends: [
    'stylelint-config-standard-scss',
    'stylelint-config-clean-order', // tri des props logiques
  ],
  rules: {
    // 1. pas de couleurs en dur sauf variables CSS
    'color-named': 'never',
    'color-no-hex': null, // on laisse temporairement, on montrera des warnings plus tard si tu veux
    'declaration-no-important': true,

    // 2. autorise les custom properties genre --tch-header-bg
    'property-no-unknown': [
      true,
      {
        ignoreProperties: [/^--tch-/, /^--mat-/, /^--comp-/],
      },
    ],

    // 3. BEM-ish classes, `tch-footer__col--brand` doit pas hurler
    'selector-class-pattern': [
      // autorise block__element--modifier
      '^[a-z][a-z0-9]*(?:-[a-z0-9]+)*(?:__(?:[a-z0-9]+(?:-[a-z0-9]+)*))?(?:--[a-z0-9]+(?:-[a-z0-9]+)*)?$',
      { message: 'Use BEM-style class names (block__element--modifier)' },
    ],

    // 4. pas d’unités interdites dans line-height
    'number-leading-zero': 'always',
  },
  ignoreFiles: ['dist/**', '**/*.js', '**/*.ts'],
};
