module.exports = {
  extends: [
    'stylelint-config-standard-scss',
    'stylelint-config-clean-order', // tri des props logiques
  ],
  plugins: ['@stylistic/stylelint-plugin'],
  rules: {
    '@stylistic/number-leading-zero': 'always',
    '@stylistic/string-quotes': 'single',
    '@stylistic/indentation': 2,
    '@stylistic/no-extra-semicolons': true,
    '@stylistic/selector-list-comma-newline-after': 'always-multi-line',
    '@stylistic/block-opening-brace-newline-after': 'always-multi-line',
    '@stylistic/declaration-colon-space-after': 'always-single-line',

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
  },
  ignoreFiles: ['dist/**', '**/*.js', '**/*.ts'],
};
