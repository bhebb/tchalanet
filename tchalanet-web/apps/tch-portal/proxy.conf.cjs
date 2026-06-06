const target = process.env.TCH_API_PROXY_TARGET || 'http://localhost:8083';

module.exports = {
  '/api': {
    target,
    secure: false,
    changeOrigin: true,
    logLevel: 'debug',
  },
};
