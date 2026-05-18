module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'body-max-line-length': [1, 'always', 100],
    'subject-case': [
      1,
      'never',
      ['pascal-case', 'upper-case']
    ]
  }
};
