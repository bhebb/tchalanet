const target = process.env.TCH_API_PROXY_TARGET || 'https://api.localtest.me';

module.exports = {
  '/api': {
    target,
    secure: false,
    changeOrigin: true,
    logLevel: 'debug',
  },
};
