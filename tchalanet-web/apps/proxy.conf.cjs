const target = process.env.TCH_API_PROXY_TARGET || 'http://localhost:8083';

module.exports = {
  '/api/v1': {
    target,
    secure: false,
    changeOrigin: true,
    logLevel: 'debug',
  },
  '/api': {
    target,
    secure: false,
    changeOrigin: true,
    logLevel: 'debug',
  },
};
