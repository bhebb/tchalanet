// eslint.config.js
import nx from '@nx/eslint-plugin';
import tsParser from '@typescript-eslint/parser';
import simpleImportSort from 'eslint-plugin-simple-import-sort';
import unusedImports from "eslint-plugin-unused-imports";

import angular from '@angular-eslint/eslint-plugin';
import angularTemplate from '@angular-eslint/eslint-plugin-template';
import templateParser from '@angular-eslint/template-parser';

export default [
  // 1. Presets Nx de base
  ...nx.configs['flat/base'],
  ...nx.configs['flat/typescript'],
  ...nx.configs['flat/javascript'],

  // 2. Fichiers ignorés globalement
  {
    ignores: [
      '**/dist',
      '**/.cache',
      '**/.angular',
      '**/vite.config.*.timestamp*',
      '**/vitest.config.*.timestamp*',
      '**/*.gen.ts',
    ],
  },

  // 3. Règles pour le code TS/JS
  {
    files: [
      '**/*.ts',
      '**/*.tsx',
      '**/*.js',
      '**/*.jsx',
      '**/*.mts',
      '**/*.cts',
      '**/*.mjs',
      '**/*.cjs',
    ],
    languageOptions: {
      parser: tsParser,
      parserOptions: {
        sourceType: 'module',
        ecmaVersion: 'latest',
      },
    },
    plugins: {
      '@angular-eslint': angular,
      'simple-import-sort': simpleImportSort,
      'unused-imports': unusedImports,
    },
    rules: {//
      // --- Nx boundaries ---
      '@nx/enforce-module-boundaries': [
        'error',
        {
          enforceBuildableLibDependency: true,
          allow: ['^.*/eslint(\\.base)?\\.config\\.[cm]?[jt]s$'],
          depConstraints: [{ sourceTag: '*', onlyDependOnLibsWithTags: ['*'] }],
        },
      ],

      // --- Hygiène TS ---
      'no-var': 'error',
      'prefer-const': 'error',

      '@typescript-eslint/no-unused-vars': 'off',
      'unused-imports/no-unused-imports': 'error',
      'unused-imports/no-unused-vars': [
        'warn',
        {
          argsIgnorePattern: '^_',
          varsIgnorePattern: '^_',
        },
      ],

      '@typescript-eslint/no-explicit-any': [
        'warn',
        { ignoreRestArgs: true },
      ],

      // --- Angular bonnes pratiques ---
      '@angular-eslint/prefer-on-push-component-change-detection': 'warn',
      '@angular-eslint/prefer-standalone': 'warn',
      '@angular-eslint/no-empty-lifecycle-method': 'warn',
      '@angular-eslint/no-host-metadata-property': 'off', // tu utilises host: {} dans certains composants header

      // --- Tri imports cohérent ---
      'simple-import-sort/imports': [
        'error',
        {
          groups: [
            // node / libs externes
            ['^node:', '^@?\\w'],
            // rxjs dédié
            ['^rxjs'],
            // angular / ngrx / cdk
            ['^@angular', '^@ngrx', '^@angular/cdk', '^@angular/material'],
            // aliases Nx internes (adapte les alias si tu en as)
            ['^@tch', '^@tchl', '^@app', '^@env'],
            // reste des imports absolus
            ['^'],
            // relatifs ../ et ./
            ['^\\.\\.(?!/?$)', '^\\.\\./?$'],
            ['^\\./(?!/?$)', '^\\./?$'],
            // assets/styles
            ['^.+\\.(s?css|svg|png|jpe?g|json)$'],
          ],
        },
      ],
      'simple-import-sort/exports': 'error',
    },
  },

  // 4. Règles spécifiques aux templates HTML externes
  {
    files: ['**/*.html'],
    languageOptions: {
      parser: templateParser,
    },
    plugins: {
      '@angular-eslint/template': angularTemplate,
    },
    rules: {
      // Accessibilité / UX
      '@angular-eslint/template/alt-text': 'error',
      '@angular-eslint/template/button-has-type': 'error',
      '@angular-eslint/template/no-autofocus': 'warn',
      '@angular-eslint/template/no-positive-tabindex': 'warn',
      '@angular-eslint/template/click-events-have-key-events': 'warn',
      '@angular-eslint/template/mouse-events-have-key-events': 'warn',
    },
  },
];
