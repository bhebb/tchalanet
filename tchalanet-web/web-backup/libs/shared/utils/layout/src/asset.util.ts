export function resolveAsset(url: string, base: string, useCdn: boolean) {
  if (!url) return url;
  if (!useCdn || /^https?:\/\//.test(url)) return url;
  return base.replace(/\/$/, '') + '/' + url.replace(/^\//, '');
}
