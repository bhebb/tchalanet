const ALLOWED_HOSTS = new Set([
  "www.galottery.com",
  "www.njlottery.com",
  "www.calottery.com",
  "www.ohiolottery.com",
  "api-solutions.ohiolottery.com",
  "authapi-solutions.ohiolottery.com",
  "www.palottery.pa.gov",
]);

const BROWSER_USER_AGENT =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

export default {
  async fetch(request, env) {
    if (request.headers.get("X-Proxy-Secret") !== env.PROXY_SHARED_SECRET) {
      return new Response("Unauthorized", { status: 401 });
    }

    const incoming = new URL(request.url);
    const target = incoming.searchParams.get("url");
    if (!target) {
      return new Response("Missing url query param", { status: 400 });
    }

    let targetUrl;
    try {
      targetUrl = new URL(target);
    } catch {
      return new Response("Invalid url query param", { status: 400 });
    }

    if (!ALLOWED_HOSTS.has(targetUrl.hostname)) {
      return new Response(`Host not allowed: ${targetUrl.hostname}`, { status: 403 });
    }

    const forwardHeaders = new Headers();
    for (const [name, value] of request.headers) {
      if (name.toLowerCase().startsWith("x-fwd-")) {
        forwardHeaders.set(name.slice("x-fwd-".length), value);
      }
    }
    if (!forwardHeaders.has("User-Agent")) {
      forwardHeaders.set("User-Agent", BROWSER_USER_AGENT);
    }

    const upstream = await fetch(targetUrl.toString(), {
      method: request.method,
      headers: forwardHeaders,
      body: ["GET", "HEAD"].includes(request.method) ? undefined : await request.arrayBuffer(),
    });

    const responseHeaders = new Headers(upstream.headers);
    responseHeaders.delete("set-cookie");

    return new Response(upstream.body, {
      status: upstream.status,
      headers: responseHeaders,
    });
  },
};
